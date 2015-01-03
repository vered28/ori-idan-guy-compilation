package LIR.Translation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import IC.DataTypes;
import IC.LiteralTypes;
import IC.AST.ASTNode;
import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.CallStatement;
import IC.AST.Continue;
import IC.AST.Expression;
import IC.AST.ExpressionBlock;
import IC.AST.Field;
import IC.AST.Formal;
import IC.AST.ICClass;
import IC.AST.If;
import IC.AST.Length;
import IC.AST.LibraryMethod;
import IC.AST.Literal;
import IC.AST.LocalVariable;
import IC.AST.Location;
import IC.AST.LogicalBinaryOp;
import IC.AST.LogicalUnaryOp;
import IC.AST.MathBinaryOp;
import IC.AST.MathUnaryOp;
import IC.AST.Method;
import IC.AST.NewArray;
import IC.AST.NewClass;
import IC.AST.PrimitiveType;
import IC.AST.Program;
import IC.AST.Return;
import IC.AST.Statement;
import IC.AST.StatementsBlock;
import IC.AST.StaticCall;
import IC.AST.StaticMethod;
import IC.AST.This;
import IC.AST.UserType;
import IC.AST.VariableLocation;
import IC.AST.VirtualCall;
import IC.AST.VirtualMethod;
import IC.AST.Visitor;
import IC.AST.While;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.ExtendedSymbol;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;
import LIR.BinaryOps;
import LIR.JumpOps;
import LIR.UnaryOps;
import LIR.Instructions.ArrayLength;
import LIR.Instructions.ArrayLoad;
import LIR.Instructions.ArrayStore;
import LIR.Instructions.BasicOperand;
import LIR.Instructions.BinaryArithmetic;
import LIR.Instructions.BinaryLogical;
import LIR.Instructions.ConstantInteger;
import LIR.Instructions.ConstantNull;
import LIR.Instructions.FieldLoad;
import LIR.Instructions.FieldStore;
import LIR.Instructions.Immediate;
import LIR.Instructions.Jump;
import LIR.Instructions.LIRClass;
import LIR.Instructions.LIRInstruction;
import LIR.Instructions.LIRMethod;
import LIR.Instructions.LIRProgram;
import LIR.Instructions.Label;
import LIR.Instructions.LibraryCall;
import LIR.Instructions.Memory;
import LIR.Instructions.Move;
import LIR.Instructions.Operand;
import LIR.Instructions.Register;
import LIR.Instructions.RegisterOffset;
import LIR.Instructions.UnaryArithmetic;

public class TranslateIC2LIR implements Visitor {

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;
	
	private ICClass currentClass = null;
	private Method currentMethod = null;
	private Statement currentStatement = null;
	
	//when assignment is x := x op something, meaning we are using
	//the same variable to update its' value:
	private boolean changingMyOwnValueOperand1 = false;
	private boolean changingMyOwnValueOperand2 = false;
		
	//keep mapping for each "live" register to its' used statement
	//or expression so we can now when to reuse it:
	private Map<Register, Statement> registerLastStatement;
	private Map<Register, Expression> registerLastExpression;
	
	//keep all already built lir-classes if you need them (for
	//inheritance, for example):
	private Map<String, LIRClass> lirclasses;

	//keep mapping of which register represents local variable of symbol
	private VariablesMap variables;
	
	private Stack<Label> labelsToBreak;
	private Stack<Label> labelsToContinue;
	
	//when handling conditions, tells to which label to jump if
	//condition is not met
	private Label jumpToLabelIfConditionIsFalse = null;
	
	//add LIR instructions as you traverse through the AST:
	private List<LIRInstruction> currentMethodInstructions = null;
	
	//save for each while (could be nested!) currentMethodInstructions
	//index of where it was when it began. Will be used to write back
	//primitive register types at the end of each iteration (if were
	//read inside loop. i.e.:
	//	while (b) {
	//		Move x,R1
	//		Add 4,R1
	//}
	//when next iteration tries to read x into R1, it should have the
	//correct value (needs to add Move R1,x at end of iteration).
	private Stack<Integer> whileInstructionIndex;
	
	private boolean supportsLibrary = false;

	public TranslateIC2LIR(StringLiteralSet literals,
			Map<ICClass, DispatchTable> dispatchTables) {
		this.literals = literals;
		this.dispatchTables = dispatchTables;
		this.lirclasses = new HashMap<String, LIRClass>();
	}
	
	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine) {
		
		Symbol symbol;
		for (Kind k : kinds) {
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine)) != null)
				return symbol;
		}

		return null;
	}
	
	/* mark in various data structures when symbol, represented now as
	 * register, is last used, so we can dispose of it at the earlier
	 * possible location.
	 */
	private void markWhenLastUsed(Symbol symbol, Register reg) {

		if (symbol instanceof ExtendedSymbol) {
			markWhenLastUsed((ExtendedSymbol)symbol, reg);
			return;
		}
		
		variables.put(symbol, reg);
		registerLastStatement.put(reg, symbol.getLastStatementUsed());
		if (symbol.getLastExpressionUsed() != null)
			registerLastExpression.put(reg, symbol.getLastExpressionUsed());

	}
	
	private void markWhenLastUsed(ExtendedSymbol symbol, Register reg) {

		variables.put(symbol, reg);
		registerLastStatement.put(reg, symbol.getLastStatementUsed(currentMethod));
		if (symbol.getLastExpressionUsed(currentMethod) != null)
			registerLastExpression.put(reg, symbol.getLastExpressionUsed(currentMethod));

	}
	
	/* decide whether to kill register (no longer needed), or to
	 * store it in map and kill it later when it is no longer needed
	 */
	private void decideIfToKillRegister(Register reg, String variableName, ASTNode node) {
		
		//first thing find the symbol representing this variable:
		Symbol symbol = findSymbol(variableName,
				Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
				node.getEnclosingScope(),
				node.getLine());
		
		if (symbol == null)
			return;
		
		if (symbol instanceof ExtendedSymbol) {
			if (((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) == null) {
				RegisterPool.putback(reg);
				return;
			}
		} else {
			if (symbol.getLastStatementUsed() == null) {
				RegisterPool.putback(reg);
				return;
			}			
		}
		
		markWhenLastUsed(symbol, reg);		
	}
	
	private void flushRegistersValuesBackToMemory(int startIndex, While whileStmt) {
		
		//only flush last move instruction to this register
		Set<Integer> registersFlushed = new HashSet<Integer>();
		
		for (int i = currentMethodInstructions.size() - 1; i >= startIndex; i--) {
			
			//if instruction is of the form Move x, R1 and x's last used
			//statement is while and x is primitive non-array, add
			//move R1,x to insturctions
			
			if (currentMethodInstructions.get(i) instanceof Move) {
				Move move = (Move)currentMethodInstructions.get(i);
				if (move.getFirstOperand() instanceof Memory &&
						move.getSecondOperand() instanceof Register) {
					
					Memory mem = (Memory)move.getFirstOperand();
					Register reg = (Register)move.getSecondOperand();
					
					if (!registersFlushed.contains(reg.getNum())) {
						
						//get symbol from register, to check last statement is current while:
						Symbol symbol = variables.getSymbol(reg);
						
						boolean used = (symbol != null && (
								((symbol instanceof ExtendedSymbol) &&
										((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) == whileStmt)
								|| (!(symbol instanceof ExtendedSymbol) && symbol.getLastStatementUsed() == whileStmt))
						);
						
						if (used) {
							
							//check if primitive non-array:
							if (symbol.getType() instanceof IC.Semantics.Types.PrimitiveType &&
									symbol.getType().getDimension() == 0) {
								currentMethodInstructions.add(new Move(
										whileStmt, reg,
										mem));
								registersFlushed.add(reg.getNum());
							}

						}
						
					}
				}
			}
			
		}
		
	}
	
	private void returnRegistersToPool(Statement stmt) {
		if (registerLastStatement.values().contains(stmt)) {
			for (Register r : registerLastStatement.keySet()) {
				if (registerLastStatement.get(r) == stmt) {
					RegisterPool.putback(r);
				}
			}
		}
	}

	private void returnRegistersToPool(Expression expr) {
		if (registerLastExpression.values().contains(expr)) {
			for (Register r : registerLastStatement.keySet()) {
				if (registerLastExpression.get(r) == expr) {
					RegisterPool.putback(r);
				}
			}
		}
	}
	
	private boolean isSymbolNoLongerInUse(Symbol symbol) {
		
		if (symbol.getKind() == Kind.FIELD) {
			ExtendedSymbol arrayExtended = (ExtendedSymbol)symbol;
			return (arrayExtended.getLastStatementUsed(currentMethod) == null
					|| (arrayExtended.getLastStatementUsed(currentMethod) == currentStatement
						&& !(currentStatement instanceof While)));
		} else {
			return (symbol.getLastStatementUsed() == null
					|| (symbol.getLastStatementUsed() == currentStatement
						&& !(currentStatement instanceof While)));
		}
		
	}
	
	@Override
	public Object visit(Program program) {
		
		//first sort classes so that the non-inheriting ones will
		//be checked first (and therefore when we reach their subclasses,
		//their own LIRClass object would have been created already).
		//plus, LIR would be "more readable" if super classes would
		//appear before subclasses and their various overrides
		List<ICClass> classes = program.getClasses();
		Collections.sort(classes, new Comparator<ICClass>() {

			@Override
			public int compare(ICClass o1, ICClass o2) {
				
				if (!o1.hasSuperClass())
					return -1;
				
				if (!o2.hasSuperClass())
					return 1;
				
				//both o1 and o2 have super classes; if one is direct
				//child of the other, sort so the parent is first.
				//otherwise, it doesn't matter.
				
				if (o1.getSuperClassName().equals(o2.getName())) {
					//o1 extends o2
					return 1;
				}
				
				if (o2.getSuperClassName().equals(o1.getName())) {
					//o2 extends o1
					return -1;
				}
				
				//does not matter
				return 0;
			}
		});
		
		LIRProgram lirprogram = new LIRProgram(
				program, literals, dispatchTables);
		
		supportsLibrary = ((ProgramScope)program.getEnclosingScope()).hasLibrary();
		
		for (ICClass cls : program.getClasses()) {
			if (cls.getName().equals("Library") && supportsLibrary) {
				//nothing to translate
				continue;
			}
			lirprogram.addClass((LIRClass)cls.accept(this));
		}
		
		return lirprogram;
	}

	@Override
	public Object visit(ICClass icClass) {
		
		currentClass = icClass;
		
		List<LIRMethod> methods = new LinkedList<LIRMethod>();
		
		for (Method method : icClass.getMethods()) {
			
			currentMethod = method;
			
			//init data structures for method translation
			//(new and separate ones for each method)!:
			this.variables = new VariablesMap();
			this.registerLastStatement = new HashMap<Register, Statement>();
			this.registerLastExpression = new HashMap<Register, Expression>();
			this.labelsToBreak = new Stack<Label>();
			this.labelsToContinue = new Stack<Label>();
			this.whileInstructionIndex = new Stack<Integer>();
			
			methods.add((LIRMethod)method.accept(this));
			
		}
		
		LIRClass lirclass;
		if (icClass.hasSuperClass()) {
			lirclass = new LIRClass(icClass,
					lirclasses.get(icClass.getSuperClassName()),
					methods);
		} else {
			lirclass = new LIRClass(icClass, methods);
		}
		lirclasses.put(icClass.getName(), lirclass);
		
		return lirclass;
	}

	@Override
	public Object visit(Field field) {
		// do nothing
		return null;
	}
	
	private void checkLastInstruction(Statement statement, Register register) {
		
		//if last instruction was call (as a signal statement), dispose of
		//its' register and set its' return to dummy:
		
		if (!(statement instanceof CallStatement))
			return;
		
		LIRInstruction instruction = currentMethodInstructions.get(
				currentMethodInstructions.size() - 1);
		
		if (instruction instanceof LIR.Instructions.Call) {
			
			if (((LIR.Instructions.Call)instruction).getReturnRegister().getNum()
					!= Register.DUMMY) {
				
				((LIR.Instructions.Call) instruction).setReturnRegister(
						new Register(instruction.getAssociactedICNode(), Register.DUMMY));
				
				RegisterPool.putback(register);
			}
		}
		
	}

	private Object visitMethod(Method method, boolean isStatic) {

		currentMethodInstructions = new LinkedList<LIRInstruction>();
		
		boolean main = false;
		
		if (isStatic && method.getName().equals("main")) {
			//semantic checks already made sure there is only
			//one main function in the whole program and that
			//it has the correct (including static) signature.
			currentMethodInstructions.add(LabelMaker.getMainLabel((StaticMethod)method));
			main = true;
		} else {
			currentMethodInstructions.add(LabelMaker.get(method,
					LabelMaker.methodString(currentClass, method)));
		}
		
		for (Statement stmt : method.getStatements()) {

			currentStatement = stmt;
			Object obj = stmt.accept(this);
			
			returnRegistersToPool(stmt);
			if (obj instanceof Register)
				checkLastInstruction(stmt, (Register)obj);
		}
		
		if (!main && method.getType() instanceof PrimitiveType &&
				((PrimitiveType)method.getType()).getPrimitiveType() == DataTypes.VOID) {
			//always be safe - put a return at the end of void functions:
			currentMethodInstructions.add(new LIR.Instructions.Return(
					method, new Register(method, Register.DUMMY)));
		}
		
		return new LIRMethod(method, currentMethodInstructions);
	}
	
	@Override
	public Object visit(VirtualMethod method) {
		return visitMethod(method, false);
	}

	@Override
	public Object visit(StaticMethod method) {
		return visitMethod(method, true);
	}

	@Override
	public Object visit(LibraryMethod method) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(Formal formal) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(PrimitiveType type) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(UserType type) {
		// do nothing
		return null;
	}

	/* checks whether or not the assignment instruction is of the form
	 * R1 = R1 + 1 (Inc) or R1 = R1 - 1 (Dec). If so replaces the last
	 * instruction (Add 1,R1) with Inc/Dec instruction and returns true.
	 * Otherwise, returns false.
	 */
	private boolean isIncOrDec(Assignment assignment, Operand var) {
		
		if (assignment.getAssignment() instanceof MathBinaryOp) {
			
			//no one promised you a bed of roses,
			//no one said it's going to be pretty,
			//shut your eyes and hope for the best,
			//this hideous sight will soon be of the past
			
			//TODO: what about arr[i]++, or this.x++;
			if (var instanceof Register) {
				
				boolean simpleMath = false;
				boolean decPossible = false; //minus not commutative!
				Symbol variableSymbol = null;
				for (Symbol sym : variables.keySet()) {
					if (variables.get(sym) == (Register)var) {
						variableSymbol = sym;
						break;
					}
				}
				
				if (variableSymbol != null) {
					
					MathBinaryOp math = ((MathBinaryOp)assignment.getAssignment());
					if (math.getFirstOperand() instanceof Literal) {
						if (((Literal)math.getFirstOperand()).getType() == LiteralTypes.INTEGER) {
							if (math.getSecondOperand() instanceof VariableLocation) {
								if (((VariableLocation)math.getSecondOperand()).getName()
										.equals(variableSymbol.getID())) {
									if (((Integer)((Literal)math.getFirstOperand()).getValue()) == 1) {
										//cha-ching! (almost...)
										simpleMath = true;
									}
								}
							}
						}
					} else if (math.getFirstOperand() instanceof VariableLocation) {
						if (math.getSecondOperand() instanceof Literal &&
								((Literal)math.getSecondOperand()).getType() == LiteralTypes.INTEGER) {
							if (((VariableLocation)math.getFirstOperand()).getName()
									.equals(variableSymbol.getID())) {
								if (Integer.valueOf(((Literal)math.getSecondOperand()).getValue() + "") == 1) {
									//cha-ching! (almost...)
									decPossible = true;
									simpleMath = true;
								}
							}
						}
					}
					
					if (simpleMath) {
						
						BinaryArithmetic lastInstruction = (BinaryArithmetic)
								currentMethodInstructions.get(
								currentMethodInstructions.size() - 1);
						Register reg = null;
						
						LIR.UnaryOps op = null;
						if (math.getOperator() == IC.BinaryOps.PLUS) {
							op = UnaryOps.Inc;
							reg = (Register)lastInstruction.getSecondOperand();
						} else if (math.getOperator() == IC.BinaryOps.MINUS) {

							if (!decPossible)
								return false;
							
							op = UnaryOps.Dec;
							//need to remove another instruction:
							currentMethodInstructions.remove(
								currentMethodInstructions.get(
												currentMethodInstructions.size() - 2));
							reg = (Register)lastInstruction.getSecondOperand();
							RegisterPool.putback((Register)lastInstruction.getSecondOperand());
						}
						
						if (op != null) {
							currentMethodInstructions.remove(lastInstruction);
							currentMethodInstructions.add(
									new UnaryArithmetic(assignment, op, reg));
							return true;
						}
					}
				}
					
			}	

		}
		
		return false;
		
	}
	
	@Override
	public Object visit(Assignment assignment) {

		//TODO: for this and local variable as well, handle the scenario:
		//		boolean b = (a < b);
		
		Operand var = (Operand)assignment.getVariable().accept(this);

		changingMyOwnValueOperand1 = (assignment.getVariable() instanceof VariableLocation
				&& assignment.getAssignment() instanceof MathBinaryOp
				&& ((MathBinaryOp)assignment.getAssignment()).getFirstOperand() instanceof VariableLocation
				&& ((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getFirstOperand())
					.getName().equals(((VariableLocation)assignment.getVariable()).getName()));
		
		changingMyOwnValueOperand2 = (assignment.getVariable() instanceof VariableLocation
				&& assignment.getAssignment() instanceof MathBinaryOp
				&& ((MathBinaryOp)assignment.getAssignment()).getSecondOperand() instanceof VariableLocation
				&& ((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getSecondOperand())
					.getName().equals(((VariableLocation)assignment.getVariable()).getName()));
				
		Object assign = assignment.getAssignment().accept(this);
		
		changingMyOwnValueOperand1 = changingMyOwnValueOperand2 = false;
		
		//check if we're doing a simple x = x + 1 or x = x - 1;
		//we can replace this using simple inc() and dec() LIR instructions
		if (isIncOrDec(assignment, var))
			return null;

		BasicOperand assignOp = null;
		//TODO: what if assignOp is register offset, array location, etc...
		if (assign instanceof BasicOperand) {
			assignOp = (BasicOperand)assign;
		} else if (assign instanceof LIR.Instructions.ArrayLocation) {
			assignOp = RegisterPool.get(assignment);
			currentMethodInstructions.add(
					new ArrayLoad(assignment,
							(LIR.Instructions.ArrayLocation)assign,
							(Register)assignOp));
		}
		
		if (var instanceof RegisterOffset) {
			
			currentMethodInstructions.add(
					new FieldStore(
							assignment, assignOp, (RegisterOffset)var));
			
			//if register offset is not longer needed, dispose of it:
			Symbol symbol = variables.getSymbol(((RegisterOffset)var).getRegister());
			if (isSymbolNoLongerInUse(symbol))
				RegisterPool.putback(((RegisterOffset)var).getRegister());
		
		} else if (var instanceof LIR.Instructions.ArrayLocation) {
			
			currentMethodInstructions.add(new ArrayStore(assignment,
					assignOp,
					(LIR.Instructions.ArrayLocation)var));
			
			if (assignOp instanceof Register)
				RegisterPool.putback((Register)assignOp);
			
		} else if (var instanceof BasicOperand) {
			
			Register reg = null;
			if (var instanceof Memory) {
				
				//only a problem if assignOp is also memory
				//(memory-to-memory is not allowed in LIR)
				if (assignOp instanceof Memory) {
					
					//first move assignOp's value to register:
					reg = RegisterPool.get(assignment);
					currentMethodInstructions.add(new Move(
							assignment, assignOp, reg));					
					
				} else if (assignOp instanceof Register) {
					//we're trying to do Move R1,x
					//check if R1 will still be used later on. If so,
					//don't move it back, keep the register:
					
					//TODO: think about external
					Location location = assignment.getVariable();
					while (location instanceof ArrayLocation) {
						location = (Location)((ArrayLocation)location).getArray();
					}

					Symbol symbol = findSymbol(((VariableLocation)location).getName(),
							Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
							assignment.getEnclosingScope(),
							assignment.getLine());
					
					//symbol != null : we passed declaration validation checks
					
					boolean stillUsed = (symbol instanceof ExtendedSymbol &&
							((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) != null &&
							((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) != assignment);
					stillUsed |= (!(symbol instanceof ExtendedSymbol) &&
							symbol.getLastStatementUsed() != null
							&& symbol.getLastStatementUsed() != assignment);
					
					if (stillUsed) {
						//we'll be needing this one a bit more...
						markWhenLastUsed(symbol, (Register)assignOp);						
						return null;
					}

				}
			}
			
			//var := assignOp (if both memory, then var := R);

			//make sure var and assign are not both the same register
			//(could happen after Math for example (R1 = R1 * 8) and
			// result was stored in R1 as well (Mul 8, R1). This is
			// correct and efficient, but we do not need a move here).
			
			boolean varIsRegister = (var instanceof Register);
			Operand assignmentOperand = (reg == null ? assignOp : reg);
			boolean assignmentIsRegister = (assignmentOperand instanceof Register);
			
			if (!varIsRegister || !assignmentIsRegister || 
					//either one is not register or both are but different:
					(((Register)var).getNum() !=
						((Register)assignmentOperand).getNum())) {
				currentMethodInstructions.add(
						new Move(assignment,
								assignmentOperand,
								var)
						);
			}
			
			if (reg != null)
				RegisterPool.putback(reg);
		
			if (assignOp instanceof Register)
				RegisterPool.putback((Register)assignOp);

		}
				
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		return callStatement.getCall().accept(this);
	}

	@Override
	public Object visit(Return returnStatement) {

		if (returnStatement.hasValue()) {
			currentMethodInstructions.add(new LIR.Instructions.Return(
					returnStatement,
					(BasicOperand)returnStatement.getValue().accept(this)));
		} else {
			currentMethodInstructions.add(new LIR.Instructions.Return(
					returnStatement,
					//return dummy value for void returns:
					new Register(returnStatement, Register.DUMMY)));
		}
		
		return null;
	}
	
	@Override
	public Object visit(If ifStatement) {
		
		Label falseLabel = LabelMaker.get(ifStatement,
				LabelMaker.labelString(currentClass, currentMethod, "_false_label"));

		Label endLabel = LabelMaker.get(ifStatement,
				LabelMaker.labelString(currentClass, currentMethod, "_end_label"));

		jumpToLabelIfConditionIsFalse =
				(ifStatement.hasElse() ? falseLabel : endLabel);
		
		Object conditionValue = ifStatement.getCondition().accept(this);
				
		Register conditionReg;
		
		if (conditionValue instanceof BasicOperand) {
			
			if (conditionValue instanceof Register)
				conditionReg = (Register)conditionValue;
			else {
				conditionReg = RegisterPool.get(ifStatement);
				currentMethodInstructions.add(
						new Move(ifStatement,
								(BasicOperand)conditionValue,
								conditionReg));
			}

			currentMethodInstructions.add(new BinaryLogical(
					ifStatement,
					BinaryOps.Compare,
					new ConstantInteger(ifStatement, 0),
					conditionReg));

			currentMethodInstructions.add(new Jump(
					ifStatement,
					JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));

			//reg is no longer needed after compare was made:
			if (!(conditionValue instanceof Register)) {
				if (conditionValue instanceof Immediate) {
					RegisterPool.putback(conditionReg);
				} else { //conditionValue is Memory..
					decideIfToKillRegister(conditionReg,
							((Memory)conditionValue).getVariableName(),
							ifStatement);
				}
			}
		}
		
		returnRegistersToPool(ifStatement.getCondition());
				
		jumpToLabelIfConditionIsFalse = null;

		Object obj = ifStatement.getOperation().accept(this);
		if (obj instanceof Register)
			checkLastInstruction(ifStatement.getOperation(), (Register)obj);
				
		if (ifStatement.hasElse()) {
			
			//after if's operation completed, jump over the else part:
			currentMethodInstructions.add(new Jump(
					ifStatement, JumpOps.Jump, endLabel));
			
			currentMethodInstructions.add(falseLabel);
			obj = ifStatement.getElseOperation().accept(this);
			if (obj instanceof Register)
				checkLastInstruction(ifStatement.getElseOperation(), (Register)obj);

		}
		
		currentMethodInstructions.add(endLabel);
		
		return null;
	}

	@Override
	public Object visit(While whileStatement) {

		Label whileLabel = LabelMaker.get(whileStatement,
				LabelMaker.labelString(currentClass,
						currentMethod, "_while_label"));

		currentMethodInstructions.add(whileLabel);
		
		whileInstructionIndex.push(currentMethodInstructions.size());
		
		Label endLabel = LabelMaker.get(whileStatement,
				LabelMaker.labelString(currentClass,
						currentMethod, "_end_label"));

		jumpToLabelIfConditionIsFalse = endLabel;
		
		Object conditionValue = whileStatement.getCondition().accept(this);
		
		Register conditionReg;
		
		if (conditionValue instanceof BasicOperand) {
			
			if (conditionValue instanceof Register)
				conditionReg = (Register)conditionValue;
			else {
				conditionReg = RegisterPool.get(whileStatement);
				currentMethodInstructions.add(
						new Move(whileStatement,
								(BasicOperand)conditionValue,
								conditionReg));
			}

			currentMethodInstructions.add(new BinaryLogical(
					whileStatement,
					BinaryOps.Compare,
					new ConstantInteger(whileStatement, 0),
					conditionReg));

			currentMethodInstructions.add(new Jump(
					whileStatement,
					JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));

			//reg is no longer needed after compare was made:
			if (!(conditionValue instanceof Register))
				RegisterPool.putback(conditionReg);
		}
		
		labelsToBreak.push(endLabel);
		labelsToContinue.push(whileLabel);
		
		Object obj = whileStatement.getOperation().accept(this);
		flushRegistersValuesBackToMemory(whileInstructionIndex.pop(), whileStatement);
		if (obj instanceof Register)
			checkLastInstruction(whileStatement.getOperation(), (Register)obj);


		labelsToBreak.pop();
		labelsToContinue.pop();
		
		currentMethodInstructions.add(new Jump(
				whileStatement, JumpOps.Jump, whileLabel));
		currentMethodInstructions.add(endLabel);

		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		currentMethodInstructions.add(new Jump(breakStatement,
				JumpOps.Jump, labelsToBreak.peek()));
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		currentMethodInstructions.add(new Jump(continueStatement,
				JumpOps.Jump, labelsToContinue.peek()));
		return null;
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) {

		for (Statement stmt : statementsBlock.getStatements()) {
			
			currentStatement = stmt;
			Object obj = stmt.accept(this);

			returnRegistersToPool(stmt);
			if (obj instanceof Register)
				checkLastInstruction(stmt, (Register)obj);
		}
		
		return null;

	}

	@Override
	public Object visit(LocalVariable localVariable) {
		
		Symbol symbol = ScopesTraversal.findSymbol(
				localVariable.getName(), Kind.VARIABLE,
				localVariable.getEnclosingScope(),
				localVariable.getLine(),
				true /* variables declared only in method scopes */);
		
		//symbol != null because we passed semantic checks
		
		if ((symbol instanceof ExtendedSymbol &&
				((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) == null) ||
			(!(symbol instanceof ExtendedSymbol) && symbol.getLastStatementUsed() == null)) {
			//variables is declared but never used
			//TODO: but... may have init value with a method call!
			return null;
		}
		
		if (localVariable.hasInitValue()) {
			
			Object initValue = localVariable.getInitValue().accept(this);
			
			Register reg = null;
			boolean needToMoveReg = true;

			if (initValue instanceof BasicOperand) {
				
				if (initValue instanceof Register) {
					if (localVariable.getInitValue() instanceof VariableLocation) {
						
						//we're doing Type x := y
						
						//check if this is the last occurrence of y. If so,
						//simply reuse it now instead of getting new register:
						Statement lastStatement = registerLastStatement.get(initValue);
						if (lastStatement == null || lastStatement == localVariable) {
							int num = ((Register)initValue).getNum();
							RegisterPool.putback((Register)initValue);
							reg = RegisterPool.get(localVariable);//(Register)initValue;
							needToMoveReg = (num != reg.getNum());
							registerLastStatement.remove(initValue);
						} else {
							reg = RegisterPool.get(localVariable);
						}
					}
					else {
						//we're doing Type x:= resultOfSomeComputationOrAllocation
						reg = (Register)initValue;
						needToMoveReg = false; //reg and initValue are same register!
					}
				} else {
					
					//only create register for this operand if it
					//requires one (is either usertype or array):
					if (symbol.getType().getDimension() > 0 ||
							symbol.getType() instanceof IC.Semantics.Types.UserType) {
						reg = RegisterPool.get(localVariable);
					}
					
					if (initValue instanceof Memory) {
						//only a problem if reg is null
						//(memory-to-memory is not allowed in LIR)
						if (reg == null) {
							reg = RegisterPool.get(localVariable);
						}
					}					
				}
				
				if (needToMoveReg) {
					currentMethodInstructions.add(
							new Move(localVariable,
									 (BasicOperand)initValue,
									 (reg == null ?
											 new Memory(localVariable, localVariable.getName())
									 		: reg)
							)
					);
				}

			} else if (initValue instanceof LIR.Instructions.ArrayLocation) {
				
				reg = RegisterPool.get(localVariable);
				
				currentMethodInstructions.add(
						new ArrayLoad(localVariable,
								(LIR.Instructions.ArrayLocation)initValue,
								reg));
				
				//TODO: think about external
				Location location = (Location)
						((LIR.Instructions.ArrayLocation)initValue)
						.getArray().getAssociactedICNode();
				
				while (location instanceof ArrayLocation) {
					location = (Location)((ArrayLocation)location).getArray();
				}
				
				decideIfToKillRegister(
						((LIR.Instructions.ArrayLocation)initValue).getArray(),
						((VariableLocation)location).getName(), localVariable);
				
			}
				

			if (reg != null) {
				markWhenLastUsed(symbol, reg);
			}
			
		}
		
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		
		//TODO: not necessarily BasicOperand, could also be offset (array / field)
		BasicOperand external = null;
		if (location.isExternal()) {
			//TODO: what if "this"...
			external = (BasicOperand)location.getLocation().accept(this);
		}
		
		ClassScope classScope = null;
		if (external != null)
			classScope = (ClassScope)ScopesTraversal.getClassScopeByName(
					location.getEnclosingScope(),
					external.getAssociactedICNode().getNodeType().getName());
		
		Symbol symbol = findSymbol(
				location.getName(),
				Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
				((external == null) ?
						location.getEnclosingScope() : classScope
				),
				location.getLine());
		
		//symbol != null since we passed DeclarationValidation semantic check
		
		switch (symbol.getKind()) {
			case VARIABLE:
			case FORMAL:
				if (variables.containsKey(symbol))
					return variables.get(symbol);
				return new Memory(location, symbol.getID());
			case FIELD:

				if (variables.containsKey(symbol))
					return variables.get(symbol);
												
				DispatchTable dt = (external == null ?
						dispatchTables.get(currentClass) :
						dispatchTables.get(
								ScopesTraversal.getICClassFromClassScope(classScope))
						);

				Register reg;

				if (location.isExternal()) {
					if (external instanceof Register) {
						reg = (Register)external;
					} else {
						reg = RegisterPool.get(location);
						currentMethodInstructions.add(new Move(location,
								external, reg));
					}
				} else {
				
					ExtendedSymbol classSymbol = (ExtendedSymbol)ScopesTraversal.findSymbol(
							currentClass.getName(), Kind.CLASS,
							((external == null) ?
									location.getEnclosingScope() : classScope
							));

					if (variables.containsKey(classSymbol))
						reg = variables.get(classSymbol);
					else {
						reg = RegisterPool.get(location);
						
						//load DV_PTR to reg				
						currentMethodInstructions.add(new Move(location,
								new Memory(location,
										(external == null ? "this" : dt.getName())
										),
								reg)
						);
						
						markWhenLastUsed(classSymbol, reg);
					}
				}
				
				//return register offset:
				return new RegisterOffset(location, reg,
								new ConstantInteger(location, dt.getOffset((Field)symbol.getNode()))
						);
				
			default:
				//never going to get here!
				return null;
		}

	}

	@Override
	public Object visit(ArrayLocation location) {

		Object array = location.getArray().accept(this);
		
		Register arrayOperand = null;
		
		if (array instanceof Register) {
			arrayOperand = (Register)array;
		} else if (array instanceof RegisterOffset) {
			arrayOperand = ((RegisterOffset)array).getRegister();

			//array is field, need to move the offset to
			//register:
			currentMethodInstructions.add(new FieldLoad(
					location,
					((RegisterOffset)array),
					arrayOperand)
			);
		} else if (array instanceof Memory) {
			//move array to reg:
			arrayOperand = RegisterPool.get(location);
			currentMethodInstructions.add(new Move(location, (Memory)array, arrayOperand));
		}
		
		Object index = location.getIndex().accept(this);

		Register tmp = null;
		if (index instanceof Memory) {
			//cannot access array via memory (only by immediate or register)
			tmp = RegisterPool.get(location);
			currentMethodInstructions.add(new Move(location,
					(Memory)index, tmp));
		}
		
		//TODO: when index is complex... (i.e. int[i+2])
		
		LIR.Instructions.ArrayLocation arrayLocation =
				new LIR.Instructions.ArrayLocation(location, arrayOperand,
						(tmp == null ? (BasicOperand)index : tmp));
		
		if (tmp != null) {
			decideIfToKillRegister(tmp,
					((Memory)index).getVariableName(), location);
		}
		
		return arrayLocation;
	}

	@Override
	public Object visit(StaticCall call) {

		//TODO: not library methods
		
		if (call.getClassName().equals("Library") && supportsLibrary) {
						
			LibraryCall libCall = new LibraryCall(call,
					"__" + call.getName(),
					null);
			
			List<Register> registersForParams = new LinkedList<Register>();
			
			for (Expression expr : call.getArguments()) {
				
				Object obj = expr.accept(this);
				
				BasicOperand param = null;
				if (obj instanceof BasicOperand) {
					param = (BasicOperand)obj;
					if (expr instanceof MathBinaryOp && param instanceof Register)
						registersForParams.add((Register)param);
				} else if (obj instanceof LIR.Instructions.ArrayLocation) {
					param = RegisterPool.get(call);
					registersForParams.add((Register)param);
					currentMethodInstructions.add(new ArrayLoad(call,
							(LIR.Instructions.ArrayLocation)obj,
							(Register)param));
				} else if (obj instanceof RegisterOffset) {
					param = RegisterPool.get(call);
					registersForParams.add((Register)param);
					currentMethodInstructions.add(new FieldLoad(call,
							(RegisterOffset)obj,
							(Register)param));					
				}
				
				libCall.addParameter(param);
			}
						
			Register returnRegister;
			if (call.getNodeType().getName().equals(DataTypes.VOID.getDescription())) {
				//return type is void, return dummy value:
				returnRegister = new Register(call, Register.DUMMY);
			} else {
				returnRegister = RegisterPool.get(call);
			}

			//put return register at the end so to keep readable and
			//coherent LIR code (since parameter calculation is done
			//before actual call, don't "hog" this register at the beginning):
			libCall.setReturnRegister(returnRegister);
			currentMethodInstructions.add(libCall);
			
			for (Register reg : registersForParams) {
				RegisterPool.putback(reg);
			}

			return returnRegister;
		}
		
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {

		//the catch for virtual call -- it may also be a static
		//call to a static method in the same class scope.
		
		boolean virtualMethod = true;
		
		Operand external = null;
		if (call.isExternal()) {
			external = (Operand)call.getLocation().accept(this);
		} else {
			
			ClassScope currentScope = (ClassScope)call.getEnclosingScope();
			Symbol staticSymbol = ScopesTraversal.findSymbol(
					call.getName(), Kind.STATICMETHOD, currentScope);
			
			if (staticSymbol != null) {
				
				Symbol virtualSymbol = ScopesTraversal.findSymbol(
						call.getName(), Kind.VIRTUALMETHOD, currentScope);
							
				if (virtualSymbol == null) {
					//this is a static call:
					virtualMethod = false;
				} else {
					
					//two methods exist with the same name in this scope, one
					//virtual and one static. Only one matches the number of
					//arguments supplied by this call (we've verified it).
					
					int staticFormalSize = ((Method)staticSymbol.getNode()).getFormals().size();
					int callArgumentsSize = call.getArguments().size();
					if (staticFormalSize == callArgumentsSize) {
						//this is a static call:
						virtualMethod = false;
					}
					
				}
			}
		}
		
		Location location = null;
		ClassScope classScope = null;
		if (external != null) {
			location = (Location)call.getLocation();
			while (location instanceof ArrayLocation) {
				location = (Location)((ArrayLocation)location).getArray();
			}
			classScope = (ClassScope)ScopesTraversal.getClassScopeByName(
					call.getEnclosingScope(),
					((IC.Semantics.Types.UserType)((VariableLocation)location).getNodeType()).getName());
		}
		
		Symbol methodSymbol = ScopesTraversal.findSymbol(
				call.getName(),
				virtualMethod ? Kind.VIRTUALMETHOD : Kind.STATICMETHOD,
				(location == null) ? call.getEnclosingScope() : classScope);
		
		
		LIR.Instructions.Call lircall;
		Register returnRegister = null;
		if (virtualMethod) {
			
			Method method = (Method)methodSymbol.getNode();
			ICClass cls = (location == null ? currentClass :
				ScopesTraversal.getICClassFromClassScope(classScope));
			
			//find offset in dispatch table:
			DispatchTable dt = dispatchTables.get(cls);
			int offset = dt.getOffset(method);
			
			//TODO: handle externals
			ExtendedSymbol classSymbol = (ExtendedSymbol)ScopesTraversal.findSymbol(
					cls.getName(), Kind.CLASS,
					(location == null ? call.getEnclosingScope() : classScope));
			
			Register thisRegister = null;
			if (variables.containsKey(classSymbol))
				thisRegister = variables.get(classSymbol);
			else if (external != null && external instanceof Register)
				thisRegister = (Register)external;
			else {
				thisRegister = RegisterPool.get(call);
				currentMethodInstructions.add(new Move(call,
						new Memory(call, (location == null ? "this" : dt.getName())),
						thisRegister));
				markWhenLastUsed(classSymbol, thisRegister);
			}
			
			lircall = new LIR.Instructions.VirtualCall(call,
					new RegisterOffset(call, thisRegister, new ConstantInteger(call, offset)),
					null);
			
			List<Register> registersForParams = new LinkedList<Register>();
			
			int count = method.getFormals().size();
			for (int i = 0; i < count; i++) {
				
				Formal formal = method.getFormals().get(i);
				
				Object argValue = call.getArguments().get(i).accept(this);

				BasicOperand param = null;
				if (argValue instanceof BasicOperand) {
					param = (BasicOperand)argValue;
				} else if (argValue instanceof LIR.Instructions.ArrayLocation) {
					param = RegisterPool.get(call);
					registersForParams.add((Register)param);
					currentMethodInstructions.add(new ArrayLoad(call,
							(LIR.Instructions.ArrayLocation)argValue,
							(Register)param));
				} else if (argValue instanceof RegisterOffset) {
					param = RegisterPool.get(call);
					registersForParams.add((Register)param);
					currentMethodInstructions.add(new FieldLoad(call,
							(RegisterOffset)argValue,
							(Register)param));					
				}
				
				((LIR.Instructions.VirtualCall)lircall).addParameter(
						new Memory(call, formal.getName()),
						(BasicOperand)argValue);
				
				if (argValue instanceof Register)
					RegisterPool.putback((Register)argValue);
				
			}

			//put return register last, so if this is statement
			//but the value is not used, be can simply move it
			//to Rdummy without it seeing as if we took higher
			//valued registers to calculate parameters:
			returnRegister = RegisterPool.get(call);
			lircall.setReturnRegister(returnRegister);

			currentMethodInstructions.add(lircall);
			
			for (Register reg : registersForParams) {
				RegisterPool.putback(reg);
			}
			
		} else {
			
			//TODO: static method
			
		}
		
		return returnRegister;
		
	}

	@Override
	public Object visit(This thisExpression) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		
		Register reg = RegisterPool.get(newClass);
		LibraryCall call = new LibraryCall(newClass, "__allocateObject", reg);
		
		Symbol classSymbol = ScopesTraversal.findSymbol(
				newClass.getName(), Kind.CLASS,
				newClass.getEnclosingScope());
		DispatchTable dispatchTable = dispatchTables.get(classSymbol.getNode());
		int fieldsCount = dispatchTable.getFields().size();
		
		//add one to fields to account for DV_PTR.
		//each field is 4 bytes, so multiply by 4:
		call.addParameter(new ConstantInteger(newClass, (fieldsCount + 1) * 4));
		currentMethodInstructions.add(call);
		
		//store DV_PTR of new class in reg.0
		currentMethodInstructions.add(
				new FieldStore(newClass,
							new Memory(newClass, dispatchTable.getName()),
							new RegisterOffset(newClass, reg,
									//offset 0:
									new ConstantInteger(newClass, 0))
				)
			);
		
		return reg;
	}

	@Override
	public Object visit(NewArray newArray) {
		
		Object val = newArray.getSize().accept(this);
		
		Register reg;
		
		if (val instanceof ConstantInteger) {
			reg = RegisterPool.get(newArray);
			LibraryCall call = new LibraryCall(newArray,
					"__allocateArray", reg);
			call.addParameter(new ConstantInteger(
					newArray, 4 * ((ConstantInteger)val).getValue()));
			currentMethodInstructions.add(call);
		} else {
			Register sizeReg;
			if (!(val instanceof Register)) {
				sizeReg = RegisterPool.get(newArray);
				currentMethodInstructions.add(new Move(
						newArray, (BasicOperand)val, sizeReg));
			} else {
				sizeReg = (Register)val;
			}
			Register multReg = RegisterPool.get(newArray);
			
			//multiply size by 4 (for each element is 4 bytes):
			//sizeReg := sizeReg * 4
			currentMethodInstructions.add(new BinaryArithmetic(newArray,
					BinaryOps.Mul,
					new ConstantInteger(newArray, 4),
					sizeReg));
			
			RegisterPool.putback(multReg);
			reg = RegisterPool.get(newArray);

			LibraryCall call = new LibraryCall(newArray,
					"__allocateArray", reg);
			call.addParameter(sizeReg);
			currentMethodInstructions.add(call);
			
			if (!(val instanceof Register))
				RegisterPool.putback(sizeReg);
		}
		
		return reg;
	}

	@Override
	public Object visit(Length length) {

		Object obj = length.getArray().accept(this);

		BasicOperand arrayOperand = null;
		if (obj instanceof BasicOperand) {
			arrayOperand = (BasicOperand)obj;
		} else if (obj instanceof LIR.Instructions.ArrayLocation) {
			arrayOperand = ((LIR.Instructions.ArrayLocation)obj).getArray();
		} else if (obj instanceof RegisterOffset) {
			
			//if register of register offset is no longer used,
			//reuse it, bitch:
			
			ExtendedSymbol sym = (ExtendedSymbol)variables.getSymbol(
					((RegisterOffset)obj).getRegister());
			
			if (sym == null ||
					sym.getLastStatementUsed(currentMethod) == currentStatement) {
				RegisterPool.putback(((RegisterOffset)obj).getRegister());
			}
			
			arrayOperand = RegisterPool.get(length);
			
			currentMethodInstructions.add(
					new FieldLoad(
							length, (RegisterOffset)obj,
							(Register)arrayOperand));
			
		}
		
		Register reg = null;
		
		if (arrayOperand instanceof Register) {
			//check if array will still be used:
			boolean stillUsed = true; //assume the worse (=cannot reuse)
			String arrayName = null;
			
			if (length.getArray() instanceof VariableLocation) {
				
				if (!((VariableLocation)length.getArray()).isExternal()) //not worth the trouble
					arrayName = ((VariableLocation)length.getArray()).getName();
			
			} else if (length.getArray() instanceof ArrayLocation) {
				
				Location location = (ArrayLocation)length.getArray();
				while (location instanceof ArrayLocation) {
					location = (Location)((ArrayLocation)location).getArray();
				}
				
				if (!((VariableLocation)location).isExternal()) //too big of a headache
					arrayName = ((VariableLocation)location).getName();
				
			}
			
			Symbol arraySymbol = null;
			if (arrayName != null) {
				
				arraySymbol = findSymbol(arrayName,
						Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
						length.getEnclosingScope(),
						length.getLine());

				stillUsed = !isSymbolNoLongerInUse(arraySymbol);
			}
			
			if (!stillUsed)
				RegisterPool.putback((Register)arrayOperand);
			else if (arraySymbol != null)
				markWhenLastUsed(arraySymbol, (Register)arrayOperand);
		}
			
		reg = RegisterPool.get(length);
		
		currentMethodInstructions.add(new ArrayLength(length, arrayOperand, reg));
		
		RegisterPool.putbackAfterNextGet(reg);
		
		return reg;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {

		//TODO: not necessary BasicOperand!
		
		Operand op1 = (Operand)binaryOp.getFirstOperand().accept(this);
		Operand op2 = (Operand)binaryOp.getSecondOperand().accept(this);
		
		boolean op1Imm = false;
		boolean op2Imm = false;
		
		if (op1 instanceof ConstantInteger)
			op1Imm = true;
		if (op2 instanceof ConstantInteger)
			op2Imm = true;
		
		if (op1Imm && op2Imm) {
			
			int val1 = ((ConstantInteger)op1).getValue();
			int val2 = ((ConstantInteger)op2).getValue();
			
			if (binaryOp.getOperator() == IC.BinaryOps.PLUS) {
				return new ConstantInteger(binaryOp, val2 + val1);
			} else if (binaryOp.getOperator() == IC.BinaryOps.MINUS) {
				return new ConstantInteger(binaryOp, val2 - val1);
			} else if (binaryOp.getOperator() == IC.BinaryOps.MULTIPLY) {
				return new ConstantInteger(binaryOp, val2 * val1);
			} else if (binaryOp.getOperator() == IC.BinaryOps.DIVIDE) {
				//TODO: check division by zero
				return new ConstantInteger(binaryOp, val2 / val1);
			} else if (binaryOp.getOperator() == IC.BinaryOps.MOD) {
				return new ConstantInteger(binaryOp, val2 % val1);
			}
		}
		
		
		Register result = null;

		//can only handle one immediate if it is either of op1, or if its' op2
		//but the action is commutative (+/*). otherwise, we can't do anything special :(
		boolean commutative = (binaryOp.getOperator() == IC.BinaryOps.PLUS ||
				binaryOp.getOperator() == IC.BinaryOps.MULTIPLY);
		
		if ((op1Imm && commutative) || op2Imm) {
			
			//both are not immediates!
			if (op2Imm) {
				//if op1 is the immediate, simply swap op1<-->op2
				//and treat both cases as the same (op1 is commutative!):
				Operand tmp = op1;
				op1 = op2;
				op2 = tmp;
			}
			
			if (changingMyOwnValueOperand1 | changingMyOwnValueOperand2) {
				if (op2 instanceof Register) {
					result = (Register)op2;
				}
			}
						
		} else {
			if (changingMyOwnValueOperand2) {
				if (op2 instanceof Register) {
					result = (Register)op2;
				}
			} else if (changingMyOwnValueOperand1) {
				//if commutative operation, we can simply swap:
				if (commutative) {
					Operand tmp = op1;
					op1 = op2;
					op2 = tmp;
					result = (Register)op2;
				} //otherwise, we can't do anything. must add another instruction :(
			}
		}
		
		if (result == null) {
			result =  RegisterPool.get(binaryOp);
			
			//move op2 value to register, unless already register
			//with the same num (reuse is happening right here, man)
			if (!(op2 instanceof Register) ||
					((Register)op2).getNum() != result.getNum())
				currentMethodInstructions.add(new Move(binaryOp, op2, result));	
		}

		//TODO: handle addition of strings (concatenation)
		
		BinaryOps op = null;
		if (binaryOp.getOperator() == IC.BinaryOps.PLUS) {
			op = BinaryOps.Add;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MINUS) {
			op = BinaryOps.Sub;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MULTIPLY) {
			op = BinaryOps.Mul;
		} else if (binaryOp.getOperator() == IC.BinaryOps.DIVIDE) {
			//TODO: check division by zero
			op = BinaryOps.Div;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MOD) {
			op = BinaryOps.Mod;
		}
		
		BasicOperand basicOp1 = null;
		boolean neededNewRegister = false;
		if (op1 instanceof BasicOperand) {
			basicOp1 = (BasicOperand)op1;
		} else if (op1 instanceof LIR.Instructions.ArrayLocation) {
			//move location to register:
			basicOp1 = RegisterPool.get(binaryOp);
			neededNewRegister = true;
			currentMethodInstructions.add(new ArrayLoad(binaryOp,
					((LIR.Instructions.ArrayLocation)op1),
					(Register)basicOp1));
		}
		
		currentMethodInstructions.add(new BinaryArithmetic(binaryOp, op, basicOp1, result));
		
		if (neededNewRegister)
			RegisterPool.putback((Register)basicOp1);
				
		return result;

	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		
		if (!binaryOp.getOperator().isLogicalOperation()) {
			
			Operand op1 = (Operand)binaryOp.getFirstOperand().accept(this);
			//TODO: op2 not necessarily basic operand
			BasicOperand op2 = (BasicOperand)binaryOp.getSecondOperand().accept(this);
			
			BasicOperand basicOp1 = null;
			if (op1 instanceof BasicOperand) {
				basicOp1 = (BasicOperand)op1;
			} else if (op1 instanceof LIR.Instructions.ArrayLocation) {
				//move array location to register:
				basicOp1 = RegisterPool.get(binaryOp); 
				currentMethodInstructions.add(new ArrayLoad(
						binaryOp,
						(LIR.Instructions.ArrayLocation)op1,
						(Register)basicOp1));
				//loading for comparison purposes, dispose of register when done:
				registerLastStatement.put((Register)basicOp1, currentStatement);
				registerLastExpression.put((Register)basicOp1, binaryOp);
			}

			//move second operand (unless it's a reuse):
			Register result = RegisterPool.get(binaryOp);
			if (!(op2 instanceof Register)
					|| ((op2 instanceof Register) && ((Register)op2).getNum() != result.getNum())) {
				currentMethodInstructions.add(new Move(binaryOp, op2, result));
			}
			
			currentMethodInstructions.add(new BinaryLogical(
					binaryOp, BinaryOps.Compare, basicOp1, result));
						
			IC.BinaryOps op = binaryOp.getOperator();
			JumpOps jump = null;
						
			//NOTE: condition is op1 * op2 (where * is <, <=, >, >=)
			//		lir asks about op-op1 value, so basically it does
			//		0 * op2-op1
			//		consider when to jump in that case (when condition
			//		DOES NOT hold!)
			
			if (op.isSizeComparisonOperation()) {
				if (op == IC.BinaryOps.LT) {
					//0 < op2-op1; complementary: 0 >= op2-op1
					jump = JumpOps.JumpLE;
				} else if (op == IC.BinaryOps.LTE){
					//0 <= op2-op1; complementary: 0 > op2-op1
					jump = JumpOps.JumpLT;
				} else if (op == IC.BinaryOps.GT){
					//0 > op2-op1; complementary: 0 <= op2-op1
					jump = JumpOps.JumpGE;
				} else if (op == IC.BinaryOps.GTE){
					//0 >= op2-op1; complementary: 0 < op2-op1
					jump = JumpOps.JumpGT;
				}
			} else if (op.isEqualityOperation()) {
				if (op == IC.BinaryOps.EQUAL) { //this one's tricky - careful!!
					//if (a == b) then compare = a-b = 0. Jump when false
					//0 == b-a --> true; complementary: false
					jump = JumpOps.JumpFalse;
				} else {
					//compare = a-b != 0 when a!=b, so if compare = 0,
					//a == b, jump (condition not met)
					jump = JumpOps.JumpTrue;
				}
			}
			
			currentMethodInstructions.add(new Jump(binaryOp, jump, jumpToLabelIfConditionIsFalse));
			RegisterPool.putback(result);
			
			if (op1 instanceof Register) {
				if (binaryOp.getFirstOperand() instanceof MathBinaryOp ||
						binaryOp.getFirstOperand() instanceof MathUnaryOp) {
							RegisterPool.putback((Register)op1);
						}
			}

			if (op2 instanceof Register) {
				if (binaryOp.getSecondOperand() instanceof MathBinaryOp ||
						binaryOp.getSecondOperand() instanceof MathUnaryOp) {
							RegisterPool.putback((Register)op2);
						}
			}

		} else { //op.isEquality() == true
			
			//TODO: not necessary BasicOperand!

			BasicOperand op1 = (BasicOperand)binaryOp.getFirstOperand().accept(this);
			Register reg1;
			if (op1 instanceof Register)
				reg1 = (Register)op1;
			else {
				reg1 = RegisterPool.get(binaryOp);
				currentMethodInstructions.add(new Move(binaryOp, op1, reg1));
			}
			
			currentMethodInstructions.add(new BinaryLogical(binaryOp,
					BinaryOps.Compare,
					new ConstantInteger(binaryOp,
							binaryOp.getOperator() == IC.BinaryOps.LOR ? 1 : 0),
					reg1));
						
			currentMethodInstructions.add(new Jump(binaryOp, JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));

			BasicOperand op2 = (BasicOperand)binaryOp.getSecondOperand().accept(this);
			Register reg2;
			if (op2 instanceof Register)
				reg2 = (Register)op2;
			else {
				reg2 = RegisterPool.get(binaryOp);
				currentMethodInstructions.add(new Move(binaryOp, op2, reg2));
			}
			
			//AND R2,R1 (R1 := R2 AND R1)
			currentMethodInstructions.add(new BinaryLogical(binaryOp,
					(binaryOp.getOperator() == IC.BinaryOps.LOR
						? BinaryOps.Or : BinaryOps.And),
					reg2, reg1));
			
			//Compare = (R1 == false)
			currentMethodInstructions.add(new BinaryLogical(binaryOp,
					BinaryOps.Compare,
					new ConstantInteger(binaryOp, 0),
					reg1));
			
			//Jump to false label if R1 is false
			currentMethodInstructions.add(new Jump(binaryOp, JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));
			
			RegisterPool.putback(reg1);
			RegisterPool.putback(reg2);
			
			if (op1 instanceof Register) {
				if (binaryOp.getFirstOperand() instanceof MathBinaryOp ||
						binaryOp.getFirstOperand() instanceof MathUnaryOp) {
							RegisterPool.putback((Register)op1);
						}
			}

			if (op2 instanceof Register) {
				if (binaryOp.getSecondOperand() instanceof MathBinaryOp ||
						binaryOp.getSecondOperand() instanceof MathUnaryOp) {
							RegisterPool.putback((Register)op1);
						}
			}

		}

		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		switch(literal.getType()) {
		case FALSE:
			return new ConstantInteger(literal, 0);
		case INTEGER:
			return new ConstantInteger(literal,
					Integer.valueOf(literal.getValue() + ""));
		case NULL:
			return new ConstantNull(literal);
		case STRING:
			return new Memory(literal,
					literals.get(literal.getValue() + "").getId());
		case TRUE:
			return new ConstantInteger(literal, 1);
		}
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		// TODO Auto-generated method stub.
		return null;
	}

}
