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
import IC.AST.Call;
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
import LIR.Instructions.UnaryLogical;

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
	
	//keep fields' symbols when they're assigned to, and flush
	//their values back to their registers when we're done
	//using them.
	private Set<ExtendedSymbol> currentMethodFieldsAssignment = null;
	private boolean lhsOfAssignmentStatement = false;
	
	//when calculating complex condition, toggle this to indicate if
	//we operating under an odd number of negations ("take negation")
	//or an even number of negations ("ignore negation").
	//for example for the condition: !(a < b) simply ask a >= b.
	//for more complicated conditions, using logical and/or, use de-morgan's
	//law: !(cond1 || cond2) is !cond1 && !cond2
	//	   !(cond1 && cond2) is !cond1 || !cond2
	//
	//more complicated example:
	//		!(a < b) && !((c < d) || !(e < f)))
	//is actually:
	//		(a >= b) && ((c >= d) && (e < f))
	//where (e < f) is under two negations, so the negations are
	//"cancelled out".
	private boolean negateCondition = false;
	
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
	
	private void printRuntimeCheckErrorAndExit(ASTNode node, Label endLabel, String errorMsg) {
		
		LibraryCall printCall = new LibraryCall(node, "__println",
				new Register(node, Register.DUMMY));
		StringLiteral sl = literals.get(errorMsg);
		if (sl == null) {
			Literal l = new Literal(0, 0, LiteralTypes.STRING, errorMsg);
			literals.add(l);
			sl = literals.get(errorMsg);
		}
		printCall.addParameter(new Memory(node, sl.getId()));
		currentMethodInstructions.add(printCall);
		
		LibraryCall exitCall = new LibraryCall(node, "__exit",
				new Register(node, Register.DUMMY));
		exitCall.addParameter(new ConstantInteger(node, 0));
		currentMethodInstructions.add(exitCall);
		
		currentMethodInstructions.add(endLabel);
		
	}
		
	private void runtimeCheckSize(ASTNode node, BasicOperand op) {
		
		Register reg = null;
		if (op instanceof Register) {
			reg = (Register)op;
		} else {
			reg = RegisterPool.get(node);
			currentMethodInstructions.add(new Move(
					node, op, reg));
		}
		
		Label endLabel = LabelMaker.get(node, 
				LabelMaker.labelString(currentClass, currentMethod,
						"_runtime_size_check_label"));

		currentMethodInstructions.add(new BinaryArithmetic(node,
				BinaryOps.Compare, new ConstantInteger(node, 0), reg));
		currentMethodInstructions.add(new Jump(node, JumpOps.JumpGE, endLabel));
		
		printRuntimeCheckErrorAndExit(node, endLabel,
				"Runtime Error: Array allocation with negative array size!");

		if (!(op instanceof Register))
			RegisterPool.putback(reg);
		
	}
	
	private void runtimeCheckOpIsZero(ASTNode node, BasicOperand op, String errMsg, String labelName) {
		
		Register reg = null;
		if (op instanceof Register) {
			reg = (Register)op;
		} else {
			reg = RegisterPool.get(node);
			currentMethodInstructions.add(new Move(
					node, op, reg));
		}
		
		Label endLabel = LabelMaker.get(node, 
				LabelMaker.labelString(currentClass, currentMethod,
						labelName));
		
		currentMethodInstructions.add(new BinaryArithmetic(node,
				BinaryOps.Compare, new ConstantInteger(node, 0), reg));
		currentMethodInstructions.add(new Jump(node, JumpOps.JumpFalse, endLabel));

		printRuntimeCheckErrorAndExit(node, endLabel, errMsg);

		if (!(op instanceof Register))
			RegisterPool.putback(reg);
	}

	
	private void runtimeCheckZero(ASTNode node, BasicOperand op) {
		runtimeCheckOpIsZero(node, op,
				"Runtime Error: Division by zero!",
				"_runtime_zero_check_label");
	}
	
	private void runtimeCheckNullRef(ASTNode node, BasicOperand op) {
		runtimeCheckOpIsZero(node, op,
				"Runtime Error: Null pointer dereference!",
				"_runtime_null_check_label");
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
				Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD, Kind.CLASS }),
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
										((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) instanceof While)
								|| (!(symbol instanceof ExtendedSymbol) && symbol.getLastStatementUsed() instanceof While))
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
	
	private void flushField(ASTNode node, ExtendedSymbol symbol, Register reg) {
		
		if (symbol.getKind() == Kind.FIELD) {
			if (currentMethodFieldsAssignment.contains(symbol)) {
				
				//need to write register value back to the field.
				//first we need to find field's offset:
				
				ExtendedSymbol classSymbol = (ExtendedSymbol)
						ScopesTraversal.findSymbol(
								currentClass.getName(), Kind.CLASS,
								node.getEnclosingScope());
				
				//classSymbol cannot be null
				
				//last used makes sure to keep the class alive in map:
				Register classReg = variables.get(classSymbol);
				int offset = dispatchTables.get(currentClass).getOffset(
						(Field)symbol.getNode());
				
				currentMethodInstructions.add(new FieldStore(node,
						reg, new RegisterOffset(node, classReg,
								new ConstantInteger(node, offset))));
			}
		}
	}
	
	private void returnRegistersToPool(Statement stmt) {
		
		List<Register> registers = new LinkedList<Register>();
		
		if (registerLastStatement.values().contains(stmt)) {
			for (Register r : registerLastStatement.keySet()) {
				if (registerLastStatement.get(r) == stmt) {

					if (variables.getSymbol(r) != null
							&& variables.getSymbol(r).getKind() == Kind.FIELD) {
						flushField(stmt, (ExtendedSymbol)variables.getSymbol(r), r);
					}
					
					registers.add(r);
					RegisterPool.putback(r);
				}
				
			}

			for (Register r : registers) {
				registerLastStatement.remove(r);
				registerLastExpression.remove(r);
			}

		}
		
	}

	private void returnRegistersToPool(Expression expr) {

		List<Register> registers = new LinkedList<Register>();

		if (registerLastExpression.values().contains(expr)) {
			for (Register r : registerLastStatement.keySet()) {
				if (registerLastExpression.get(r) == expr) {
					
					if (variables.getSymbol(r) != null
							&& variables.getSymbol(r).getKind() == Kind.FIELD) {
						flushField(expr, (ExtendedSymbol)variables.getSymbol(r), r);
					}

					registers.add(r);
					RegisterPool.putback(r);
					
				}
			}

			for (Register r : registers) {
				registerLastStatement.remove(r);
				registerLastExpression.remove(r);
			}

		}
	}
	
	private boolean isSymbolNoLongerInUse(Symbol symbol) {
		
		if (symbol instanceof ExtendedSymbol) {
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
			
			this.currentMethodFieldsAssignment = new HashSet<ExtendedSymbol>();
						
			methods.add((LIRMethod)method.accept(this));
			
			RegisterPool.flushPool();
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
		
		if (assignment.getAssignment() instanceof MathBinaryOp
				&& assignment.getVariable() instanceof VariableLocation) {
			
			//no one promised you a bed of roses,
			//no one said it's going to be pretty,
			//shut your eyes and hope for the best,
			//this hideous sight will soon be of the past
			
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
										if (matchExternals((VariableLocation)assignment.getVariable(),
												(VariableLocation)math.getSecondOperand())) {
											//cha-ching! (almost...)
											simpleMath = true;
										}
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
									if (matchExternals((VariableLocation)assignment.getVariable(),
											(VariableLocation)math.getFirstOperand())) {
										//cha-ching! (almost...)
										decPossible = true;
										simpleMath = true;
									}
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
							reg = (Register)lastInstruction.getSecondOperand();
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
	
	/*
	 * checks that externals of two variable locations match, given that both locations
	 * have the same name. Meaning, checks that both refer to the same symbol.
	 */
	private boolean matchExternals(VariableLocation loc1, VariableLocation loc2) {
		
		if (!loc1.isExternal() && !loc2.isExternal())
			return true;
		
		if (!loc1.isExternal() || !loc2.isExternal()) {
			//are only allowed not to match if one of them is "this"
			//(we can do this.x = x + 1) if both refer to the same x!
			
			if (loc2.isExternal()) {
				//swap loc1<-->loc2 (so we can handle both cases in the same code):
				VariableLocation tmp = loc1;
				loc1 = loc2;
				loc2 = tmp;
			}
			
			if (!(loc1.getLocation() instanceof This))
				return false;
			//need to ensure loc2's variable name only exists as a field:
			Symbol symbol = findSymbol(loc2.getName(),
					Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL } ),
					loc2.getEnclosingScope(), loc2.getLine());
			
			//if symbol is null, both refer to the class field:
			return (symbol == null);
		}
		
		//both have externals... need to ensure they match:
		
		if (loc1.getLocation() instanceof This && loc2.getLocation() instanceof This)
			return true;
		
		//not handling externals that are arrays: requires index checking that's
		//also a bit of an overhead...
		if (!(loc1.getLocation() instanceof VariableLocation) ||
				!(loc2.getLocation() instanceof VariableLocation))
			return false;
		
		//it's always enough to compare names in assignment - both lhs and rhs
		//are in the same specific scope, therefore both refer to the same symbol.
		return ((VariableLocation)loc1.getLocation()).getName().equals(
				((VariableLocation)loc2.getLocation()).getName());
	}
	
	/*
	 * check if expression is new array expression and if so, returns the NewArray
	 * expression. otherwise returns null.
	 */
	private NewArray isNewArray(Expression expr)  {
		NewArray newArray = null;
		if (expr instanceof ArrayLocation) {
			ArrayLocation arrayLocation = (ArrayLocation)expr;
			while (true) {
				Expression array = arrayLocation.getArray();
				if (!(array instanceof ArrayLocation)) {
					if (array instanceof NewArray) {
						newArray = (NewArray)array;
					}
					break;
				}
				arrayLocation = (ArrayLocation)array;
			}
		}
		return newArray;
	}
	
	@Override
	public Object visit(Assignment assignment) {
		
		//raise flags if assignment of variable to itself (i.e. x = x + y).
		//the following checks for non-array locations (we don't have special handling for a[i] = a[i] + y).
		
		changingMyOwnValueOperand1 = (assignment.getVariable() instanceof VariableLocation
				&& assignment.getAssignment() instanceof MathBinaryOp
				&& ((MathBinaryOp)assignment.getAssignment()).getFirstOperand() instanceof VariableLocation
				&& ((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getFirstOperand())
					.getName().equals(((VariableLocation)assignment.getVariable()).getName())
				&& matchExternals((VariableLocation)assignment.getVariable(),
						((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getFirstOperand())));
		
		changingMyOwnValueOperand2 = (assignment.getVariable() instanceof VariableLocation
				&& assignment.getAssignment() instanceof MathBinaryOp
				&& ((MathBinaryOp)assignment.getAssignment()).getSecondOperand() instanceof VariableLocation
				&& ((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getSecondOperand())
					.getName().equals(((VariableLocation)assignment.getVariable()).getName())
				&& matchExternals((VariableLocation)assignment.getVariable(),
						((VariableLocation)((MathBinaryOp)assignment.getAssignment()).getSecondOperand())));
		
		// find out if assignment is binary logical expression (unary does not require
		// jumps on its own). If so, set jump label and jump.
		Expression expr = assignment.getAssignment();
		//could be !!!!(a < b)
		while ((expr instanceof ExpressionBlock) || (expr instanceof LogicalUnaryOp)) {
			if (expr instanceof ExpressionBlock)
				expr = ((ExpressionBlock)expr).getExpression();
			else
				expr = ((LogicalUnaryOp)expr).getOperand();
		}
		
		boolean booleanAssignment = (expr instanceof LogicalBinaryOp);
		Label assignmentLabel = null;
		
		if (booleanAssignment) {
			assignmentLabel = LabelMaker.get(assignment,
					LabelMaker.labelString(currentClass, currentMethod, "_false_boolean_assign_label"));
			jumpToLabelIfConditionIsFalse = assignmentLabel;
		}
		
		//first run assignment and then run variable - not the other way around.
		//it may not really matter, however if assignment is using the variable
		//in its' expression, it may load it to a register and therefore
		//calling variable after assignment is the better approach.
		//helps us prevent a case where for field x the statement x = x + z,
		//is translated to:
		//		MoveField R1.1, R2
		//		Move R2, R3
		//		Add z,R3
		//		Move R3, R1.1
		// (when x is used again later [which is also why we use R3 and don't
		//  simply override R2 with the calculation's value])
		Object assign = assignment.getAssignment().accept(this);
		lhsOfAssignmentStatement = true;
		Operand var = (Operand)assignment.getVariable().accept(this);
		lhsOfAssignmentStatement = false;
		
		changingMyOwnValueOperand1 = changingMyOwnValueOperand2 = false;
		if (booleanAssignment)
			jumpToLabelIfConditionIsFalse = null;
		
		//check if we're doing a simple x = x + 1 or x = x - 1;
		//we can replace this using simple inc() and dec() LIR instructions
		if (isIncOrDec(assignment, var))
			return null;

		BasicOperand assignOp = null;
		if (assign instanceof BasicOperand) {
			assignOp = (BasicOperand)assign;
		} else if (assign instanceof LIR.Instructions.ArrayLocation) {
			assignOp = RegisterPool.get(assignment);
			currentMethodInstructions.add(
					new ArrayLoad(assignment,
							(LIR.Instructions.ArrayLocation)assign,
							(Register)assignOp));
		} else if (assign instanceof RegisterOffset) {
			assignOp = RegisterPool.get(assignment);
			currentMethodInstructions.add(
					new FieldLoad(assignment,
							(RegisterOffset)assign,
							(Register)assignOp));
		}
				
		if (var instanceof RegisterOffset) {
			
			currentMethodInstructions.add(
					new FieldStore(
							assignment, assignOp, (RegisterOffset)var));
			
			if (assignment.getAssignment() instanceof NewArray) {
				markWhenLastUsed(((RegisterOffset)var).getSymbol(), (Register)assignOp);
			} else if (assignOp instanceof Register) {
				//push back if no longer needed:
				Symbol sym = variables.getSymbol((Register)assignOp);
				if (sym == null ||
						//or 'variables' information is garbage - not relevant to assignOp...
						((var instanceof Memory) && !sym.getID().equals(((Memory)var).getVariableName())) ||
						//or this is the last statement using this symbol in this method
						isSymbolNoLongerInUse(sym))
					RegisterPool.putback((Register)assignOp);
			}
			
			//if register offset is not longer needed, dispose of it:
			Symbol symbol = variables.getSymbol(((RegisterOffset)var).getRegister());
			if (symbol == null || isSymbolNoLongerInUse(symbol))
				RegisterPool.putback(((RegisterOffset)var).getRegister());
		
		} else if (var instanceof LIR.Instructions.ArrayLocation) {
			
			currentMethodInstructions.add(new ArrayStore(assignment,
					assignOp,
					(LIR.Instructions.ArrayLocation)var));
			
			if (assignOp instanceof Register)
				RegisterPool.putback((Register)assignOp);
			
			//if array is not longer needed, dispose of it:
			Symbol symbol = variables.getSymbol(((LIR.Instructions.ArrayLocation)var).getArray());
			if (symbol != null && isSymbolNoLongerInUse(symbol))
				RegisterPool.putback(((LIR.Instructions.ArrayLocation)var).getArray());
			
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
					//don't move it back, keep the register (of course
					//for that to be relevant, we'd need R1 symbol to be x):
					
					//(todo not dealt with removed): think about external
					Location location = assignment.getVariable();
					while (location instanceof ArrayLocation) {
						location = (Location)((ArrayLocation)location).getArray();
					}

					Symbol symbol = findSymbol(((VariableLocation)location).getName(),
							Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
							assignment.getEnclosingScope(),
							assignment.getLine());
					
					//symbol != null : we passed declaration validation checks
					
					Symbol registerSymbol = variables.getSymbol((Register)assignOp);
					
					//if registerSymbol == null, assignOp is a temporary register...
					//of course, this could also be garbage, ensure assignOp is
					//register for var...
					//also ensure: if assign is array location or register offset,
					//we just grabbed this register from the pool and might want
					//to keep it if the variable we're assigning will still be used.
					
					boolean newAssignRegister = (assign instanceof RegisterOffset ||
							assign instanceof LIR.Instructions.ArrayLocation);
					
					if (registerSymbol == null || newAssignRegister ||
							registerSymbol.getID().equals(((VariableLocation)location).getName())) {
										
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
			}
			
			if (booleanAssignment) {
				
				//true:
				currentMethodInstructions.add(new Move(assignment,
						new ConstantInteger(assignment, 1), var));
				Label endLabel = LabelMaker.get(assignment,
						LabelMaker.labelString(currentClass, currentMethod, "_end_assignment"));
				currentMethodInstructions.add(new Jump(assignment, JumpOps.Jump, endLabel));
				
				//false:
				currentMethodInstructions.add(assignmentLabel);
				currentMethodInstructions.add(new Move(assignment,
						new ConstantInteger(assignment, 0), var));
				currentMethodInstructions.add(endLabel);
				
			} else {
			
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
			}
			
			if (reg != null)
				RegisterPool.putback(reg);
		
			if (assignOp instanceof Register) {
				//push back if no longer needed:
				Symbol sym = variables.getSymbol((Register)assignOp);
				if (sym == null ||
						//or 'variables' information is garbage - not relevant to assignOp...
						((var instanceof Memory) && !sym.getID().equals(((Memory)var).getVariableName())) ||
						//or this is the last statement using this symbol in this method
						isSymbolNoLongerInUse(sym))
					RegisterPool.putback((Register)assignOp);
			}

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
			
			Operand op = (Operand)returnStatement.getValue().accept(this);
			
			BasicOperand returnOperand = null;
			if (op instanceof BasicOperand) {
				returnOperand = (BasicOperand)op;
			} else if (op instanceof RegisterOffset) {
				returnOperand = RegisterPool.get(returnStatement);
				currentMethodInstructions.add(new FieldLoad
						(returnStatement, (RegisterOffset)op,
								(Register)returnOperand));
			} else if (op instanceof LIR.Instructions.ArrayLocation) {
				returnOperand = RegisterPool.get(returnStatement);
				currentMethodInstructions.add(new ArrayLoad
						(returnStatement,
								(LIR.Instructions.ArrayLocation)op,
								(Register)returnOperand));				
			}
			
			currentMethodInstructions.add(new LIR.Instructions.Return(
					returnStatement, returnOperand));
			
			if (op instanceof Register) {
				if (returnStatement.getValue() instanceof MathBinaryOp ||
						returnStatement.getValue() instanceof ExpressionBlock)
					RegisterPool.putback((Register)op);
			}
			
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

	private boolean localVariableInitValueContainsCalls(Expression initValue) {

		if (initValue instanceof Call) {
			
			if (initValue instanceof StaticCall) {
				
				StaticCall call = (StaticCall)initValue;
				Symbol symbol = ScopesTraversal.findSymbol(call.getName(),
						Kind.STATICMETHOD,
						ScopesTraversal.getClassScopeByName(
								call.getEnclosingScope(),
								call.getClassName()));
				
				//if method is pure, we can ignore this call...
				return !((Method)symbol.getNode()).isPure();
				
			} else if (initValue instanceof VirtualCall) {
				
				//need to distinguish between virtual and yet another
				//static possibility:
				
				VirtualCall call = (VirtualCall)initValue;
				
				boolean virtualMethod = true;
				
				if (call.isExternal()) {
					//we assume externals hurt pureness
					return true;
				}
					
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

				Symbol methodSymbol = ScopesTraversal.findSymbol(
						call.getName(),
						virtualMethod ? Kind.VIRTUALMETHOD : Kind.STATICMETHOD,
						call.getEnclosingScope());
				
				return !((Method)methodSymbol.getNode()).isPure();
			}
			
			return true;
		}
		
		if (initValue instanceof ExpressionBlock) {
			return localVariableInitValueContainsCalls(
					((ExpressionBlock)initValue).getExpression());
		}
		
		if (initValue instanceof MathBinaryOp) {
			boolean op1 = localVariableInitValueContainsCalls(
					((MathBinaryOp)initValue).getFirstOperand());
			boolean op2 = localVariableInitValueContainsCalls(
					((MathBinaryOp)initValue).getSecondOperand());
			return op1 || op2;
		}

		if (initValue instanceof LogicalBinaryOp) {
			boolean op1 = localVariableInitValueContainsCalls(
					((LogicalBinaryOp)initValue).getFirstOperand());
			boolean op2 = localVariableInitValueContainsCalls(
					((LogicalBinaryOp)initValue).getSecondOperand());
			return op1 || op2;
		}
		
		if (initValue instanceof MathUnaryOp) {
			return localVariableInitValueContainsCalls(
					((MathUnaryOp)initValue).getOperand());
		}

		if (initValue instanceof LogicalUnaryOp) {
			return localVariableInitValueContainsCalls(
					((LogicalUnaryOp)initValue).getOperand());
		}
		
		if (initValue instanceof Length) {
			return localVariableInitValueContainsCalls(
					((Length)initValue).getArray());
		}
		
		return false;

	}
	
	private Object initNewArray(Expression expr, NewArray newArray) {
		
		if (expr instanceof NewArray) {
			//one dimensional array...
			return newArray.accept(this);
		}
		
		List<Register> registersToDispose = new LinkedList<Register>();

		List<BasicOperand> dimensionSizes = new LinkedList<BasicOperand>();
		Map<BasicOperand, Register> basics = new HashMap<BasicOperand, Register>();
		
		Expression array = expr;
		while (array instanceof ArrayLocation) {
			Object size = ((ArrayLocation)array).getIndex().accept(this);
			Register sizeReg = null;
			if (size instanceof Register) {
				sizeReg = (Register)size;
			} else {
				sizeReg = RegisterPool.get(newArray);
				registersToDispose.add(sizeReg);
				if (size instanceof RegisterOffset) {
					currentMethodInstructions.add(new FieldLoad(
							newArray, (RegisterOffset)size, sizeReg));
				} else if (size instanceof LIR.Instructions.ArrayLocation) {
					currentMethodInstructions.add(new ArrayLoad(
							newArray,
							(LIR.Instructions.ArrayLocation)size,
							sizeReg));
				} else {
					boolean alreadyAdded = false;
					if (size instanceof ConstantInteger) {
						for (BasicOperand bo : basics.keySet()) {
							if (bo instanceof ConstantInteger &&
									((ConstantInteger)bo).getValue() == ((ConstantInteger)size).getValue()) {
								alreadyAdded = true;
								dimensionSizes.add(0, basics.get(bo));
								break;
							}
						}
					} else if (size instanceof Memory) {
						for (BasicOperand bo : basics.keySet()) {
							if (bo instanceof Memory &&
									((Memory)bo).getVariableName().equals(((Memory)size).getVariableName())) {
								alreadyAdded = true;
								dimensionSizes.add(0, basics.get(bo));
								break;								
							}
						}
					}
					
					if (!alreadyAdded) {
						runtimeCheckSize(newArray, sizeReg);
						currentMethodInstructions.add(new Move(newArray,
								(BasicOperand)size, sizeReg));
						basics.put((BasicOperand)size, sizeReg);
						dimensionSizes.add(0, sizeReg);
					} else {
						RegisterPool.putback(sizeReg);
						registersToDispose.remove(sizeReg);
					}
				}
			}
			array = ((ArrayLocation)array).getArray();
		}
		
		//if we have new array[size1][size2][size3]..
		//we need to init an array of size1, and then go over 0 to size1-1
		//of its' indexes and init each one of them with an array of size2
		//and then its' one of its' indexes as an array of size3.
		
		Register reg = (Register)createNewArray(newArray,
				((NewArray)array).getSize().accept(this));
		
		int count = dimensionSizes.size();
		Register[] arrays = new Register[count+1];
		arrays[0] = reg;

		Register[] registers = new Register[count];
		
		Stack<Label> labels = new Stack<Label>();
		
		for (int i = 0; i < count; i++) {
			
			Register r1 = RegisterPool.get(newArray);
			registersToDispose.add(r1);
			registers[i] = r1;
			
			currentMethodInstructions.add(new Move(newArray,
					new ConstantInteger(newArray, 0), r1));
			
			Label condLabel = LabelMaker.get(newArray, LabelMaker.labelString(
					currentClass, currentMethod, "_allocation_while"));
			
			Label endLabel = LabelMaker.get(newArray, LabelMaker.labelString(
					currentClass, currentMethod, "_allocation_end"));
			
			labels.push(endLabel);
			labels.push(condLabel);
			
			currentMethodInstructions.add(condLabel);
			
			BasicOperand bo = dimensionSizes.get(i);
			Register r2 = null;
			if (bo instanceof Register) {
				r2 = (Register)bo;
			} else {
				r2 = RegisterPool.get(newArray);
				registersToDispose.add(r2);
				currentMethodInstructions.add(new Move(newArray, bo, r2));
			}
			
			currentMethodInstructions.add(new BinaryLogical(newArray,
					BinaryOps.Compare, r1, r2));
			currentMethodInstructions.add(new Jump(newArray, JumpOps.JumpLE, endLabel));
			
			LibraryCall libcall = new LibraryCall(newArray, "__allocateArray", null);
			Register tmp = RegisterPool.get(newArray);
			registersToDispose.add(tmp);
			currentMethodInstructions.add(new Move(newArray, dimensionSizes.get(i), tmp));
			currentMethodInstructions.add(new BinaryArithmetic(newArray, BinaryOps.Mul,
					new ConstantInteger(newArray, 4), tmp));
			libcall.addParameter(tmp);
			libcall.setReturnRegister(tmp);
			arrays[i+1] = tmp;
			currentMethodInstructions.add(libcall);
			
		}
		
		for (int i = count - 1; i >= 0; i--) {
			
			currentMethodInstructions.add(new ArrayStore(newArray, arrays[i+1],
					new LIR.Instructions.ArrayLocation(newArray, arrays[i], registers[i])));
			currentMethodInstructions.add(new UnaryArithmetic(newArray, UnaryOps.Inc, registers[i]));
			currentMethodInstructions.add(new Jump(newArray, JumpOps.Jump, labels.pop()));
			currentMethodInstructions.add(labels.pop());
			
		}
		
		for (Register r : registersToDispose) {
			RegisterPool.putback(r);
		}
		
		return reg;
	}
	
	@Override
	public Object visit(LocalVariable localVariable) {
		
		Symbol symbol = ScopesTraversal.findSymbol(
				localVariable.getName(), Kind.VARIABLE,
				localVariable.getEnclosingScope(),
				localVariable.getLine(),
				true /* variables declared only in method scopes */);
		
		//symbol != null because we passed semantic checks
		
		boolean unusedDeclaration = false;
		
		if ((symbol instanceof ExtendedSymbol &&
				((ExtendedSymbol)symbol).getLastStatementUsed(currentMethod) == null) ||
			(!(symbol instanceof ExtendedSymbol) && symbol.getLastStatementUsed() == null)) {
			//variables is declared but never used
			//but... may have init value with a method call! (god only knows what he's
			//expecting that method to do, for instance, to fields... must call that method anyway):
			if (!localVariable.hasInitValue() ||
					!localVariableInitValueContainsCalls(localVariable.getInitValue())) {
				return null;
			}
			unusedDeclaration = true;
		}
		
		if (localVariable.hasInitValue()) {
			
			// find out if init value is binary logical expression (unary does not require
			// jumps on its own). If so, set jump label and jump.
			Expression expr = localVariable.getInitValue();
			//could be !!!!(a < b)
			while ((expr instanceof ExpressionBlock) || (expr instanceof LogicalUnaryOp)) {
				if (expr instanceof ExpressionBlock)
					expr = ((ExpressionBlock)expr).getExpression();
				else
					expr = ((LogicalUnaryOp)expr).getOperand();
			}
			
			boolean booleanInit = (expr instanceof LogicalBinaryOp);
			Label initLabel = null;
			
			if (booleanInit) {
				initLabel = LabelMaker.get(localVariable,
						LabelMaker.labelString(currentClass, currentMethod, "_false_boolean_init_label"));
				jumpToLabelIfConditionIsFalse = initLabel;
			}
			
			Object initValue;
			NewArray newArray = isNewArray(localVariable.getInitValue());
			if (newArray != null) {
				initValue = initNewArray(localVariable.getInitValue(), newArray);
			} else {			
				initValue = localVariable.getInitValue().accept(this);
			}
			
			if (booleanInit) {
				
				jumpToLabelIfConditionIsFalse = null;

				//true:
				currentMethodInstructions.add(new Move(localVariable,
						new ConstantInteger(localVariable, 1),
						new Memory(localVariable, localVariable.getName())));
				Label endLabel = LabelMaker.get(localVariable,
						LabelMaker.labelString(currentClass, currentMethod, "_init_assignment"));
				currentMethodInstructions.add(new Jump(localVariable, JumpOps.Jump, endLabel));
				
				//false:
				currentMethodInstructions.add(initLabel);
				currentMethodInstructions.add(new Move(localVariable,
						new ConstantInteger(localVariable, 0),
						new Memory(localVariable, localVariable.getName())));
				currentMethodInstructions.add(endLabel);
			}
			
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
							reg = RegisterPool.get(localVariable);
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
					//only move if not both ends of the assignment are the same register:
					if (reg == null || !(initValue instanceof Register) || (initValue instanceof Register
							&& reg.getNum() != ((Register)initValue).getNum())) {
						currentMethodInstructions.add(
								new Move(localVariable,
										 (BasicOperand)initValue,
										 (reg == null ?
												 new Memory(localVariable, localVariable.getName())
										 		: reg)
								)
						);
					}
				}

			} else if (initValue instanceof LIR.Instructions.ArrayLocation) {
				
				reg = RegisterPool.get(localVariable);
								
				currentMethodInstructions.add(
						new ArrayLoad(localVariable,
								(LIR.Instructions.ArrayLocation)initValue,
								reg));
				
				//(todo not dealt with removed): think about external
				Location location = (Location)localVariable.getInitValue();
				
				while (location instanceof ArrayLocation) {
					location = (Location)((ArrayLocation)location).getArray();
				}
				
				decideIfToKillRegister(
						((LIR.Instructions.ArrayLocation)initValue).getArray(),
						((VariableLocation)location).getName(), localVariable);
				
			}
			
			if (localVariable.getInitValue() instanceof Call && unusedDeclaration) {
				
				LIRInstruction inst = currentMethodInstructions.get(
						currentMethodInstructions.size() - 1);
						
				if (inst instanceof LIR.Instructions.Call) {
					Register returnRegister = ((LIR.Instructions.Call)inst).getReturnRegister();
					RegisterPool.putback(returnRegister);
					((LIR.Instructions.Call)inst).setReturnRegister(
							new Register(localVariable, Register.DUMMY));
				}
			}
				

			if (reg != null) {
				markWhenLastUsed(symbol, reg);
			}
			
		}
		
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		
		BasicOperand external = null;
		boolean thisKeyword = false;
		IC.Semantics.Types.Type externalType = null;

		if (location.isExternal()) {
			
			Object externalValue = location.getLocation().accept(this);
			thisKeyword = (location.getLocation() instanceof This);
			
			if (!thisKeyword) {
				
				if (externalValue instanceof BasicOperand) {
				
					external = (BasicOperand)externalValue;
					externalType = external.getAssociactedICNode().getNodeType();
					
				} else if (externalValue instanceof RegisterOffset) {
					
					Register reg = RegisterPool.get(location);
					externalType = ((RegisterOffset)externalValue).
							getAssociactedICNode().getNodeType();
					currentMethodInstructions.add(new FieldLoad(location,
							(RegisterOffset)externalValue, reg));
					external = reg;
					
					markWhenLastUsed(((RegisterOffset)externalValue).getSymbol(), reg);
					
				} else if (externalValue instanceof LIR.Instructions.ArrayLocation) {
					
					Register reg = RegisterPool.get(location);
					externalType = ((LIR.Instructions.ArrayLocation)externalValue)
							.getAssociactedICNode().getNodeType();
					currentMethodInstructions.add(new ArrayLoad(location,
							(LIR.Instructions.ArrayLocation)externalValue,
							reg));
					external = reg;
					
				}
			}
			
			if (external != null)
				runtimeCheckNullRef(location, external);
		}
		
		ClassScope classScope = null;
		if (external != null) {
			if (thisKeyword) {
				classScope = (ClassScope)currentClass.getEnclosingScope();
			} else {
				classScope = (ClassScope)ScopesTraversal.getClassScopeByName(
					location.getEnclosingScope(),
					externalType.getName());
			}
		}
		
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

				//if lefthand side of assignment into field of current class (no externals
				//since they are objects and only matter in their own scope):
				if (lhsOfAssignmentStatement && (thisKeyword || external == null)) {
					//only keep track of primitive non-array types:
					if (symbol.getType() instanceof IC.Semantics.Types.PrimitiveType
							&& symbol.getType().getDimension() == 0) {
						currentMethodFieldsAssignment.add((ExtendedSymbol)symbol);
					}
				}
				
				if (variables.containsKey(symbol))
					return variables.get(symbol);
												
				DispatchTable dt = (external == null ?
						dispatchTables.get(currentClass) :
						dispatchTables.get(
								ScopesTraversal.getICClassFromClassScope(classScope))
						);

				Register reg;

				if (location.isExternal() && !thisKeyword) {
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
								new ConstantInteger(location, dt.getOffset((Field)symbol.getNode())),
								symbol
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
			
			//do we need to dispose and reuse of register?:
			Register reg = ((RegisterOffset)array).getRegister();
			if (isSymbolNoLongerInUse(variables.getSymbol(reg))) {
				RegisterPool.putbackAfterNextGet(reg);
				registerLastStatement.remove(reg);
				registerLastExpression.remove(reg);
			}

			//array is field, need to move the offset to register:
			arrayOperand = RegisterPool.get(location);
			currentMethodInstructions.add(new FieldLoad(
					location,
					((RegisterOffset)array),
					arrayOperand)
			);
			
			decideIfToKillRegister(arrayOperand,
					((RegisterOffset)array).getSymbol().getID(),
					location);
						
		} else if (array instanceof LIR.Instructions.ArrayLocation) {
			//multidimensional array
			arrayOperand = RegisterPool.get(location);			
			currentMethodInstructions.add(new ArrayLoad(
					location, (LIR.Instructions.ArrayLocation)array, arrayOperand));
			
		} else if (array instanceof Memory) {
			//move array to reg:
			arrayOperand = RegisterPool.get(location);
			currentMethodInstructions.add(new Move(location, (Memory)array, arrayOperand));
		}
		
		runtimeCheckNullRef(location, arrayOperand);
		
		Object index = location.getIndex().accept(this);

		Register tmp = null;
		if (index instanceof Memory) {
			//cannot access array via memory (only by immediate or register)
			tmp = RegisterPool.get(location);
			currentMethodInstructions.add(new Move(location,
					(Memory)index, tmp));
		}
		
		LIR.Instructions.ArrayLocation arrayLocation =
				new LIR.Instructions.ArrayLocation(location, arrayOperand,
						(tmp == null ? (BasicOperand)index : tmp));
		
		if (tmp != null) {
			decideIfToKillRegister(tmp,
					((Memory)index).getVariableName(), location);
		}
		
		return arrayLocation;
	}

	private Object doStaticCall(Call call, LIR.Instructions.Call lircall, StaticMethod staticMethod) {
		
		boolean library = (lircall instanceof LibraryCall);
		
		List<Register> registersForParams = new LinkedList<Register>();
		
		int count = call.getArguments().size();
		for (int i = 0; i < count; i++) {
			
			Expression expr = call.getArguments().get(i);
			
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
			
			if (library) {
				((LibraryCall)lircall).addParameter(param);
			} else {
				((LIR.Instructions.StaticCall)lircall).addParameter(
						new Memory(call, staticMethod.getFormals().get(i).getName()),
						(BasicOperand)obj);
			}
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
		lircall.setReturnRegister(returnRegister);
		currentMethodInstructions.add(lircall);
		
		for (Register reg : registersForParams) {
			RegisterPool.putback(reg);
		}

		return returnRegister;
		
	}
	
	@Override
	public Object visit(StaticCall call) {

		LIR.Instructions.Call lircall;
		StaticMethod staticMethod = null;
		
		if (call.getClassName().equals("Library") && supportsLibrary) {
		
			lircall = new LibraryCall(call,
					"__" + call.getName(),
					null);
		} else {
			
			lircall = new LIR.Instructions.StaticCall(
					call,
					//regenerate label for static method:
					LabelMaker.methodString(call.getClassName(), call.getName(), true),
					null);
			
			ClassScope scope = (ClassScope)ScopesTraversal.getClassScopeByName(
					call.getEnclosingScope(), call.getClassName());
			Symbol method = scope.getStaticSymbol(call.getName());
			staticMethod = (StaticMethod)method.getNode();
			
		}
		
		return doStaticCall(call, lircall, staticMethod);
		
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
			
			ExtendedSymbol classSymbol = (ExtendedSymbol)ScopesTraversal.findSymbol(
					cls.getName(), Kind.CLASS,
					(location == null ? call.getEnclosingScope() : classScope));
			
			Register thisRegister = null;
			if (external != null && external instanceof Register) {
				thisRegister = (Register)external;
			} else if (external != null && external instanceof LIR.Instructions.ArrayLocation) {
				thisRegister = RegisterPool.get(call);
				currentMethodInstructions.add(new ArrayLoad(call,
						(LIR.Instructions.ArrayLocation)external,
						thisRegister));
			} else if (variables.containsKey(classSymbol)) {
				thisRegister = variables.get(classSymbol);
			} else {
				thisRegister = RegisterPool.get(call);
				currentMethodInstructions.add(new Move(call,
						new Memory(call, (location == null ? "this" : ((VariableLocation)location).getName())),
						thisRegister));
				markWhenLastUsed(classSymbol, thisRegister);
			}
			
			runtimeCheckNullRef(call, thisRegister);
			
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
			//but the value is not used, we can simply move it
			//to Rdummy without it seeing as if we took higher
			//valued registers to calculate parameters:
			returnRegister = RegisterPool.get(call);
			lircall.setReturnRegister(returnRegister);

			if (external != null && external instanceof LIR.Instructions.ArrayLocation)
				RegisterPool.putback(thisRegister);
			
			currentMethodInstructions.add(lircall);
			
			for (Register reg : registersForParams) {
				RegisterPool.putback(reg);
			}
			
		} else {
			
			lircall = new LIR.Instructions.StaticCall(
					call,
					//regenerate label for static method:
					LabelMaker.methodString(currentClass.getName(), call.getName(), true),
					null);
			
			doStaticCall(call, lircall, (StaticMethod)methodSymbol.getNode());

		}
		
		return returnRegister;
		
	}

	@Override
	public Object visit(This thisExpression) {

		//returns register pointing to this class DV_PTR.
		
		//check if register already exists:
		Symbol symbol = ScopesTraversal.findSymbol(currentClass.getName(),
				Kind.CLASS, thisExpression.getEnclosingScope());
		
		if (symbol != null && variables.containsKey(symbol))
			return variables.get(symbol);
		
		//symbol does not exist, move this to new register, map it and store it:
		Register reg = RegisterPool.get(thisExpression);
		currentMethodInstructions.add(new Move(thisExpression,
				new Memory(thisExpression, "this"), reg));
		decideIfToKillRegister(reg, currentClass.getName(), thisExpression);
		
		return reg;
		
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

	private Object createNewArray(ASTNode newArray, Object val) {
		
		Register reg;
		
		if (val instanceof ConstantInteger) {
			reg = RegisterPool.get(newArray);
			LibraryCall call = new LibraryCall(newArray,
					"__allocateArray", reg);
			//size is definitely positive, since we checked in
			//semantic checks (for easy to know types such as const int).
			call.addParameter(new ConstantInteger(
					newArray, 4 * ((ConstantInteger)val).getValue()));
			currentMethodInstructions.add(call);
		} else {
			Register sizeReg;
			if (!(val instanceof Register)) {
				sizeReg = RegisterPool.get(newArray);
				if (val instanceof RegisterOffset) {
					currentMethodInstructions.add(new FieldLoad(
							newArray, (RegisterOffset)val, sizeReg));
				} else if (val instanceof LIR.Instructions.ArrayLocation) {
					currentMethodInstructions.add(new ArrayLoad(
							newArray, (LIR.Instructions.ArrayLocation)val,
							sizeReg));
				} else {
					currentMethodInstructions.add(new Move(
							newArray, (BasicOperand)val, sizeReg));
				}
			} else {
				sizeReg = (Register)val;
			}
			
			runtimeCheckSize(newArray, sizeReg);
			
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
	public Object visit(NewArray newArray) {		
		return createNewArray(newArray, 
				newArray.getSize().accept(this));
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
		
		runtimeCheckNullRef(length, arrayOperand);
		
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

		int weight1 = binaryOp.getFirstOperand().getWeight();
		int weight2 = binaryOp.getSecondOperand().getWeight();
		
		Operand op1, op2;
		if (weight1 >= weight2 || weight1 == -1 || weight2 == -1) {
			op1 = (Operand)binaryOp.getFirstOperand().accept(this);
			op2 = (Operand)binaryOp.getSecondOperand().accept(this);
		} else {
			//first do op2
			op2 = (Operand)binaryOp.getSecondOperand().accept(this);
			op1 = (Operand)binaryOp.getFirstOperand().accept(this);			
		}
		
		//LIR is weird.. doing op1 @ op2 needs to be written as
		//@ op2,op1. so let us simply swap the two...
		{
			Operand tmp = op1;
			op1 = op2;
			op2 = tmp;
			
			boolean tmpChanging = changingMyOwnValueOperand1;
			changingMyOwnValueOperand1 = changingMyOwnValueOperand2;
			changingMyOwnValueOperand2 = tmpChanging;
		}
		
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
				//when possible we've checked val1 != 0 in semantic checks!
				return new ConstantInteger(binaryOp, val2 / val1);
			} else if (binaryOp.getOperator() == IC.BinaryOps.MOD) {
				return new ConstantInteger(binaryOp, val2 % val1);
			}
		}
		
		
		boolean neededNewop2Register = false;
		boolean neededNewop1Register = false;

		if (op2 instanceof LIR.Instructions.ArrayLocation) {
			//move location to register:
			Register tmp = RegisterPool.get(binaryOp);
			neededNewop2Register = true;
			currentMethodInstructions.add(new ArrayLoad(binaryOp,
					((LIR.Instructions.ArrayLocation)op2),
					tmp));
			op2 = tmp;
		} else if (op2 instanceof RegisterOffset) {
			Register tmp = RegisterPool.get(binaryOp);
			currentMethodInstructions.add(new FieldLoad(binaryOp,
					(RegisterOffset)op2, tmp));
			
			if (isSymbolNoLongerInUse(((RegisterOffset)op2).getSymbol())) {
				neededNewop2Register = true;
			} else {
				markWhenLastUsed(((RegisterOffset)op2).getSymbol(), tmp);
			}
			
			op2 = tmp;
		}

		if (op1 instanceof LIR.Instructions.ArrayLocation) {
			//move location to register:
			Register tmp = RegisterPool.get(binaryOp);
			neededNewop1Register = true;
			currentMethodInstructions.add(new ArrayLoad(binaryOp,
					((LIR.Instructions.ArrayLocation)op1),
					tmp));
			op1 = tmp;
		} else if (op1 instanceof RegisterOffset) {
			Register tmp = RegisterPool.get(binaryOp);
			currentMethodInstructions.add(new FieldLoad(binaryOp,
					(RegisterOffset)op1, tmp));
			
			if (isSymbolNoLongerInUse(((RegisterOffset)op1).getSymbol())) {
				neededNewop1Register = true;
			} else {
				markWhenLastUsed(((RegisterOffset)op1).getSymbol(), tmp);
			}
			
			op1 = tmp;
		}

		if (binaryOp.getNodeType() instanceof IC.Semantics.Types.PrimitiveType &&
				((IC.Semantics.Types.PrimitiveType)binaryOp.getNodeType()).getType() == DataTypes.STRING) {
			
			//we've validated types already - if on operand is string
			//both are and this is a PLUS operation (i.e. string concatenation)
			
			LibraryCall call = new LibraryCall(binaryOp, "__stringCat", null);

			call.addParameter((BasicOperand)op2);
			call.addParameter((BasicOperand)op1);

			Register reg = RegisterPool.get(binaryOp);
			call.setReturnRegister(reg);
			currentMethodInstructions.add(call);
			
			if (neededNewop2Register)
				RegisterPool.putback((Register)op2);

			if (neededNewop1Register)
				RegisterPool.putback((Register)op1);

			return reg;
			
		}
		
		Register result = null;
		
		boolean swapped = false;

		//can only handle one immediate if it is either of op2, or if its' op1
		//but the action is commutative (+/*). otherwise, we can't do anything special :(
		boolean commutative = (binaryOp.getOperator() == IC.BinaryOps.PLUS ||
				binaryOp.getOperator() == IC.BinaryOps.MULTIPLY);
		
		if ((op2Imm && commutative) || op1Imm) {
			
			//both are not immediates!
			if (op1Imm) {
				//if op1 is the immediate, simply swap op2<-->op1
				//and treat both cases as the same (op2 is commutative!):
				Operand tmp = op2;
				op2 = op1;
				op1 = tmp;
				
				swapped = true;
				
				boolean tmp2 = neededNewop2Register;
				neededNewop2Register = neededNewop1Register;
				neededNewop1Register = tmp2;
			}
			
			if (changingMyOwnValueOperand1 | changingMyOwnValueOperand2) {
				if (op1 instanceof Register) {
					result = (Register)op1;
				}
			}
						
		} else {
			if (changingMyOwnValueOperand2) {
				if (op1 instanceof Register) {
					result = (Register)op1;
				}
			} else if (changingMyOwnValueOperand1) {
				//if commutative operation, we can simply swap:
				if (commutative) {
					Operand tmp = op2;
					op2 = op1;
					op1 = tmp;
					result = (Register)op1;
					swapped = true;
				} //otherwise, we can't do anything. must add another instruction :(
			}
		}
		
		boolean makeFirstOperandFirst = false;
		
		if (swapped) { //remember we swapped originally!
			if (binaryOp.getSecondOperand() instanceof MathBinaryOp)
				result = (Register)op1;
			else if (binaryOp.getSecondOperand() instanceof ExpressionBlock)
				result = (Register)op1;
		} else {
			if (binaryOp.getFirstOperand() instanceof MathBinaryOp) {
				makeFirstOperandFirst = true;
				result = (Register)op2;
			}
			else if (binaryOp.getFirstOperand() instanceof ExpressionBlock) {
				makeFirstOperandFirst = true;
				result = (Register)op2;
			}
		}
		
		if (result == null) {
			result =  RegisterPool.get(binaryOp);
			
			//move op1 value to register, unless already register
			//with the same num (reuse is happening right here, man)
			if (!(op1 instanceof Register) ||
					((Register)op1).getNum() != result.getNum())
				currentMethodInstructions.add(new Move(binaryOp, op1, result));						
		}
		
		BinaryOps op = null;
		if (binaryOp.getOperator() == IC.BinaryOps.PLUS) {
			op = BinaryOps.Add;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MINUS) {
			op = BinaryOps.Sub;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MULTIPLY) {
			op = BinaryOps.Mul;
		} else if (binaryOp.getOperator() == IC.BinaryOps.DIVIDE) {
			runtimeCheckZero(binaryOp, (BasicOperand)op2); //op a,b does b/a
			op = BinaryOps.Div;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MOD) {
			op = BinaryOps.Mod;
		}
				
		currentMethodInstructions.add(new BinaryArithmetic(binaryOp, op, 
				(makeFirstOperandFirst ? (BasicOperand)op1 : (BasicOperand)op2), result));
		
		if (neededNewop2Register)
			RegisterPool.putback((Register)op2);

		if (neededNewop1Register)
			RegisterPool.putback((Register)op1);

		return result;

	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		
		if (!binaryOp.getOperator().isLogicalOperation()) {
			
			int weight1 = binaryOp.getFirstOperand().getWeight();
			int weight2 = binaryOp.getFirstOperand().getWeight();
			
			Operand op1, op2;
			if (weight1 >= weight2 || weight1 == -1 || weight2 == -1) {
				op1 = (Operand)binaryOp.getFirstOperand().accept(this);
				op2 = (Operand)binaryOp.getSecondOperand().accept(this);
			} else {
				//first do op2
				op2 = (Operand)binaryOp.getSecondOperand().accept(this);
				op1 = (Operand)binaryOp.getFirstOperand().accept(this);			
			}
			
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
			} else if (op1 instanceof RegisterOffset) {
				//move register offset to register:
				basicOp1 = RegisterPool.get(binaryOp); 
				currentMethodInstructions.add(new FieldLoad(
						binaryOp,
						(RegisterOffset)op1,
						(Register)basicOp1));
				//loading for comparison purposes, dispose of register when done:
				registerLastStatement.put((Register)basicOp1, currentStatement);
				registerLastExpression.put((Register)basicOp1, binaryOp);				
			}

			//move second to register (if not already register):
			Register result = null;
			if (op2 instanceof Register)
				result = (Register)op2;
			else {
				result = RegisterPool.get(binaryOp);
				if (op2 instanceof RegisterOffset) {
					currentMethodInstructions.add(new FieldLoad(
							binaryOp, (RegisterOffset)op2, result));
				} else if (op2 instanceof LIR.Instructions.ArrayLocation) {
					currentMethodInstructions.add(new ArrayLoad(
							binaryOp, (LIR.Instructions.ArrayLocation)op2,
							result));
				} else {
					currentMethodInstructions.add(new Move(
							binaryOp, op2, result));
				}
			}
			
			currentMethodInstructions.add(new BinaryLogical(
					binaryOp, BinaryOps.Compare, basicOp1, result));
						
			IC.BinaryOps op = binaryOp.getOperator();
			JumpOps jump = null;
						
			//NOTE: condition is op1 @ op2 (where @ is <, <=, >, >=)
			//		lir asks about op-op1 value, so basically it does
			//		0 @ op2-op1
			//		consider when to jump in that case (when condition
			//		DOES NOT hold!)
			
			if (op.isSizeComparisonOperation()) {
				if (op == IC.BinaryOps.LT) {
					if (!negateCondition) {
						//0 < op2-op1; complementary: 0 >= op2-op1
						jump = JumpOps.JumpLE;
					} else {
						//0 >= op2-op1; negation is: 0 < op2-op1
						jump = JumpOps.JumpGT;
					}
				} else if (op == IC.BinaryOps.LTE){
					if (!negateCondition) {
						//0 <= op2-op1; complementary: 0 > op2-op1
						jump = JumpOps.JumpLT;
					} else {
						//0 > op2-op1; negation is: 0 <= op2-op1
						jump = JumpOps.JumpGE;
					}
				} else if (op == IC.BinaryOps.GT){
					if (!negateCondition) {
						//0 > op2-op1; complementary: 0 <= op2-op1
						jump = JumpOps.JumpGE;
					} else {
						//0 <= op2-op1; negation is 0 > op2-op1
						jump = JumpOps.JumpLT;
					}
				} else if (op == IC.BinaryOps.GTE){
					if (!negateCondition) {
						//0 >= op2-op1; complementary: 0 < op2-op1
						jump = JumpOps.JumpGT;
					} else {
						//0 < op2-op1; negation is 0 >= op2-op1
						jump = JumpOps.JumpLE;
					}
				}
			} else if (op.isEqualityOperation()) {
				if (op == IC.BinaryOps.EQUAL) { //this one's tricky - careful!!
					if (!negateCondition) {
						//if (a == b) then compare = a-b = 0. Jump when false
						//0 == b-a --> true; complementary: false
						jump = JumpOps.JumpFalse;
					} else {
						jump = JumpOps.JumpTrue;
					}
				} else {
					if (!negateCondition) {
						//compare = a-b != 0 when a!=b, so if compare = 0,
						//a == b, jump (condition not met)
						jump = JumpOps.JumpTrue;
					} else {
						jump = JumpOps.JumpFalse;
					}
				}
			}
			
			currentMethodInstructions.add(new Jump(binaryOp, jump, jumpToLabelIfConditionIsFalse));
			
			if (!(op2 instanceof Register))
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
			
			boolean operand1IsConditional = (binaryOp.getFirstOperand() instanceof LogicalBinaryOp);
			if (!operand1IsConditional) {
				//could still be unary... however, could be something
				//crazy such as !!!!!!!!!(a op b) or !!!!!!!!b
				//we only want to mark the first case as conditional,
				//the second case would return register value...
				Expression op1expr = binaryOp.getFirstOperand();
				while (op1expr instanceof LogicalUnaryOp || op1expr instanceof ExpressionBlock) {
					if (op1expr instanceof LogicalUnaryOp)
						op1expr = ((LogicalUnaryOp)op1expr).getOperand();
					else
						op1expr = ((ExpressionBlock)op1expr).getExpression();
				}
				operand1IsConditional = (op1expr instanceof LogicalBinaryOp);
			}
			
			//when operand1 is conditional (we're evaluating a complex condition:
			//		(cond1 op cond2) op cond3 etc...
			//if OR instruction: if the 1st operand fails (is false), don't give
			//up, give it another chance: don't jump to condition's false label
			//but to a new label "mid-way" so that we can give the 2nd operand
			//a chance to take this condition to true. However, if AND instruction,
			//we only get one strike...
			
			boolean orOperation = (binaryOp.getOperator() == IC.BinaryOps.LOR);
			if (negateCondition)
				orOperation = !orOperation; //if AND do OR; if OR do AND.
			
			Label originalFalseLabel = jumpToLabelIfConditionIsFalse;
			Label secondOperandLabel = null;
			
			Label endLabel = LabelMaker.get(binaryOp,
					LabelMaker.labelString(currentClass, currentMethod, "_or_end_label"));
			
			if (orOperation && operand1IsConditional) {

				secondOperandLabel = LabelMaker.get(
						binaryOp, LabelMaker.labelString(currentClass, currentMethod, "_or_label"));
				jumpToLabelIfConditionIsFalse = secondOperandLabel;
				
			}

			Register reg1 = null; 
			Register reg2 = null;
			
			Operand op1 = (Operand)binaryOp.getFirstOperand().accept(this);
			
			if (op1 == null) {
				//binaryOp.getFirstOperand() is LogicalBinary/UnaryOperation
				//--> list of instructions; at the end of their execution, if
				//condition holds (true), we execute next instruction. if
				//condition is false, then we jump to label
				//(for OR, secondOperandLabel; for AND jumpToLabelIfConditionIsFalse)
				
				//if AND instruction --> nothing else to do, we jumped to correct place.
				//if OR instruction --> jump to end (condition holds), put label and
				//						try the second operand:
				
				if (orOperation) {
					currentMethodInstructions.add(new Jump(binaryOp, JumpOps.Jump, endLabel));
					currentMethodInstructions.add(secondOperandLabel);
					jumpToLabelIfConditionIsFalse = originalFalseLabel;					
				}
				
			} else {
				
				if (op1 instanceof Register)
					reg1 = (Register)op1;
				else {
					reg1 = RegisterPool.get(binaryOp);
					if (op1 instanceof RegisterOffset) {
						currentMethodInstructions.add(
								new FieldLoad(binaryOp,
									(RegisterOffset)op1,
									reg1));
					} else if (op1 instanceof LIR.Instructions.ArrayLocation) {
						currentMethodInstructions.add(
								new ArrayLoad(binaryOp,
									(LIR.Instructions.ArrayLocation)op1,
									reg1));
					} else {
						currentMethodInstructions.add(
								new Move(binaryOp, op1, reg1));						
					}
				}

				//compare R1 == (true if or / false if and)
				currentMethodInstructions.add(new BinaryLogical(binaryOp,
						BinaryOps.Compare,
						new ConstantInteger(binaryOp,
								orOperation ? 1 : 0),
						reg1));

				currentMethodInstructions.add(new Jump(binaryOp, JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));
				
			}
			
			Operand op2 = (Operand)binaryOp.getSecondOperand().accept(this);
			
			if (op2 == null) {
				//binaryOp.getSecondOperand() is LogicalBinary/UnaryOperation
				//--> list of instructions; at the end of their execution,
				//if condition holds (true), we execute next instruction.
				//if condition is false, then we jump to label.
			} else {
				
				if (op2 instanceof Register)
					reg2 = (Register)op2;
				else {
					reg2 = RegisterPool.get(binaryOp);
					if (op2 instanceof RegisterOffset) {
						currentMethodInstructions.add(
								new FieldLoad(binaryOp,
									(RegisterOffset)op2,
									reg2));
					} else if (op2 instanceof LIR.Instructions.ArrayLocation) {
						currentMethodInstructions.add(
								new ArrayLoad(binaryOp,
									(LIR.Instructions.ArrayLocation)op2,
									reg2));
					} else {
						currentMethodInstructions.add(new Move(binaryOp, op2, reg2));
					}
				}
				
				//Compare = (R2 == true)
				currentMethodInstructions.add(new BinaryLogical(binaryOp,
						BinaryOps.Compare,
						new ConstantInteger(binaryOp, 1),
						reg2));
				
				//Jump to false label if R2 is false
				currentMethodInstructions.add(new Jump(binaryOp, JumpOps.JumpTrue, jumpToLabelIfConditionIsFalse));
				
				if (reg1 != null)
					RegisterPool.putback(reg1);
				if (reg2 != null)
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
			
			if (operand1IsConditional && orOperation)
				currentMethodInstructions.add(endLabel);

		}

		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {

		//this is simply -Expr...
				
		Operand operand = (Operand)unaryOp.getOperand().accept(this);
		
		Register reg = null;
		if (operand instanceof BasicOperand) {
			
			if (operand instanceof Register) {
				
				boolean registerStillInUse = true; //default assume the worse (cannot reuse);
				if (unaryOp.getOperand() instanceof MathBinaryOp ||
						unaryOp.getOperand() instanceof MathUnaryOp) {
					registerStillInUse = false; //we can dispose intermediate value
				} else if (variables.getSymbol((Register)operand) != null) {
					if (isSymbolNoLongerInUse(variables.getSymbol((Register)operand))) {
						registerStillInUse = false;
					}
				}
				
				if (!registerStillInUse)
					RegisterPool.putback((Register)operand);
				
				reg = RegisterPool.get(unaryOp);
						
			} else if (operand instanceof ConstantInteger) {
				return new ConstantInteger(unaryOp,
						-((ConstantInteger)operand).getValue());
			} else {
				//move value to register:
				reg = RegisterPool.get(unaryOp);
				currentMethodInstructions.add(new Move(unaryOp, operand, reg));
			}
			
		} else if (operand instanceof LIR.Instructions.ArrayLocation) {
			reg = RegisterPool.get(unaryOp);
			currentMethodInstructions.add(new ArrayLoad(unaryOp,
					(LIR.Instructions.ArrayLocation)operand, reg));			
		} else if (operand instanceof RegisterOffset) {
			reg = RegisterPool.get(unaryOp);
			currentMethodInstructions.add(new FieldLoad(unaryOp,
					(RegisterOffset)operand, reg));			
		}
		
		currentMethodInstructions.add(new UnaryArithmetic(unaryOp, UnaryOps.Neg, reg));
		
		return reg;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {

		//!expr
		
		//if !!!!!!(binaryOperation) simply toggle negation switch:
		Expression expr = unaryOp.getOperand();
		boolean toggled = false;
		while (expr instanceof LogicalUnaryOp || expr instanceof ExpressionBlock) {
			if (expr instanceof LogicalUnaryOp)
				expr = ((LogicalUnaryOp)expr).getOperand();
			else
				expr = ((ExpressionBlock)expr).getExpression();
		}
		if (expr instanceof LogicalBinaryOp) {
			toggled = true;
			negateCondition = !negateCondition;
		}

		Operand operand = (Operand)unaryOp.getOperand().accept(this);
		
		if (toggled) {
			//toggle back:
			negateCondition = !negateCondition;
		}
		
		Register reg = null;
		if (operand == null) {
			return null;
		} else if (operand instanceof BasicOperand) {
			
			if (operand instanceof Register) {
				
				boolean registerStillInUse = true; //default assume the worse (cannot reuse);
				if (unaryOp.getOperand() instanceof LogicalBinaryOp ||
						unaryOp.getOperand() instanceof LogicalUnaryOp) {
					registerStillInUse = false; //we can dispose intermediate value
				} else if (variables.getSymbol((Register)operand) != null) {
					if (isSymbolNoLongerInUse(variables.getSymbol((Register)operand))) {
						registerStillInUse = false;
					}
				}
				
				if (!registerStillInUse)
					RegisterPool.putback((Register)operand);
				
				reg = RegisterPool.get(unaryOp);
						
			} else if (operand instanceof ConstantInteger) {
				//true is 1, false is 0 ; !true=1-1=0=false, !false=1-0=1=true
				return new ConstantInteger(unaryOp,
						1-((ConstantInteger)operand).getValue());
			} else {
				//move value to register:
				reg = RegisterPool.get(unaryOp);
				currentMethodInstructions.add(new Move(unaryOp, operand, reg));
			}
		} else if (operand instanceof LIR.Instructions.ArrayLocation) {
			reg = RegisterPool.get(unaryOp);
			currentMethodInstructions.add(new ArrayLoad(unaryOp,
					(LIR.Instructions.ArrayLocation)operand, reg));			
		} else if (operand instanceof RegisterOffset) {
			reg = RegisterPool.get(unaryOp);
			currentMethodInstructions.add(new FieldLoad(unaryOp,
					(RegisterOffset)operand, reg));			
		}
		
		currentMethodInstructions.add(new UnaryLogical(unaryOp, reg));
		
		return reg;
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
		return expressionBlock.getExpression().accept(this);
	}

}
