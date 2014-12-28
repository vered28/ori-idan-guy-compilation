package LIR.Translation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;
import LIR.BinaryOps;
import LIR.JumpOps;
import LIR.Instructions.ArrayStore;
import LIR.Instructions.BasicOperand;
import LIR.Instructions.BinaryArithmetic;
import LIR.Instructions.BinaryInstruction;
import LIR.Instructions.BinaryLogical;
import LIR.Instructions.ConstantInteger;
import LIR.Instructions.ConstantNull;
import LIR.Instructions.FieldStore;
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

public class TranslateIC2LIR implements Visitor {

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;
	
	private ICClass currentClass = null;
	private Method currentMethod = null;
		
	//keep mapping for each "live" register to its' used statement
	//or expression so we can now when to reuse it:
	private Map<Register, Statement> registerLastStatement;
	private Map<Register, Expression> registerLastExpression;
	
	//keep all already built lir-classes if you need them (for
	//inheritance, for example):
	private Map<String, LIRClass> lirclasses;

	//keep mapping of which register represents local variable of symbol
	private Map<Symbol, Register> variables;
	
	private Stack<Label> labelsToBreak;
	private Stack<Label> labelsToContinue;
	
	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine) {
		
		Symbol symbol;
		for (Kind k : kinds) {
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine)) != null)
				return symbol;
		}

		return null;
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
	
	public TranslateIC2LIR(StringLiteralSet literals,
			Map<ICClass, DispatchTable> dispatchTables) {

		this.literals = literals;
		this.dispatchTables = dispatchTables;

		this.registerLastStatement = new HashMap<Register, Statement>();
		this.registerLastExpression = new HashMap<Register, Expression>();
		this.lirclasses = new HashMap<String, LIRClass>();
		this.variables = new HashMap<Symbol, Register>();
		
		this.labelsToBreak = new Stack<Label>();
		this.labelsToContinue = new Stack<Label>();
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
		
		for (ICClass cls : program.getClasses()) {
			if (cls.getName().equals("Library")) continue;
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

	@SuppressWarnings("unchecked")
	public Object visitMethod(Method method, boolean isStatic) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		if (isStatic && method.getName().equals("main")) {
			//semantic checks already made sure there is only
			//one main function in the whole program and that
			//it has the correct (including static) signature.
			instructions.add(LabelMaker.getMainLabel((StaticMethod)method));
		} else {
			instructions.add(LabelMaker.get(method,
					LabelMaker.methodString(currentClass, method)));
		}
		
		for (Statement stmt : method.getStatements()) {
			instructions.addAll((List<LIRInstruction>)stmt.accept(this));
			returnRegistersToPool(stmt);
		}
		
		return new LIRMethod(method, instructions);
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

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(Assignment assignment) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		Operand var = (Operand)assignment.getVariable().accept(this);
		Object assign = assignment.getAssignment().accept(this);
		
		BasicOperand assignOp;
		if (assign instanceof List<?>) {
			List<LIRInstruction> insts = (List<LIRInstruction>)assign;
			assignOp = (Register)((BinaryInstruction)insts.get(insts.size() - 1)).getSecondOperand();
			instructions.addAll(insts);
		} else {
			assignOp = (BasicOperand)assign;
		}
		
		if (var instanceof RegisterOffset) {
			instructions.add(new FieldStore(assignment, assignOp, (RegisterOffset)var));
		} else if (var instanceof LIR.Instructions.ArrayLocation) {
			instructions.add(new ArrayStore(assignment, assignOp,
					(LIR.Instructions.ArrayLocation)var));
		} else if (var instanceof Register) {
			instructions.add(new Move(assignment, assignOp, var));
		}
		
		if (assign instanceof List<?>)
			RegisterPool.putback((Register)assignOp);
		
		return instructions;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		return callStatement.getCall().accept(this);
	}

	@Override
	public Object visit(Return returnStatement) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		if (returnStatement.hasValue()) {
			instructions.add(new LIR.Instructions.Return(returnStatement,
					(BasicOperand)returnStatement.getValue().accept(this)));
		} else {
			instructions.add(new LIR.Instructions.Return(returnStatement,
					new Register(returnStatement, Register.DUMMY)));
		}
		
		return instructions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(If ifStatement) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		Object conditionValue = ifStatement.getCondition().accept(this);
		
		Register conditionReg;
		if (conditionValue instanceof List<?>) {
			List<LIRInstruction> insts = ((List<LIRInstruction>)conditionValue);
			Move moveInst = (Move)insts.get(insts.size() - 2);
			conditionReg = (Register)moveInst.getSecondOperand();
			instructions.addAll(insts);
		} else {
			if (conditionValue instanceof Register)
				conditionReg = (Register)conditionValue;
			else {
				conditionReg = RegisterPool.get(ifStatement);
				instructions.add(new Move(ifStatement,
						(BasicOperand)conditionValue, conditionReg));
			}
		}		
		instructions.add(new BinaryLogical(
				ifStatement,
				BinaryOps.Compare,
				new ConstantInteger(ifStatement, 0),
				conditionReg));
				
		//reg is no longer needed after compare was made:
		RegisterPool.putback(conditionReg);
		
		Label endLabel = LabelMaker.get(ifStatement,
				LabelMaker.labelString(currentClass, currentMethod, "_end_label"));
		
		if (ifStatement.hasElse()) {
			
			Label falseLabel = LabelMaker.get(ifStatement,
					LabelMaker.labelString(currentClass, currentMethod, "_false_label"));
			
			instructions.add(new Jump(ifStatement, JumpOps.JumpTrue, falseLabel));
			instructions.addAll(
					(List<LIRInstruction>)ifStatement.getOperation().accept(this));
			
			instructions.add(new Jump(ifStatement, JumpOps.Jump, endLabel));
			instructions.add(falseLabel);
			instructions.addAll(
					(List<LIRInstruction>)ifStatement.getElseOperation().accept(this));
			
		} else {
			instructions.add(new Jump(ifStatement, JumpOps.JumpTrue, endLabel));
			instructions.addAll(
					(List<LIRInstruction>)ifStatement.getOperation().accept(this));
		}
		
		instructions.add(endLabel);
		
		return instructions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(While whileStatement) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();

		Label whileLabel = LabelMaker.get(whileStatement,
				LabelMaker.labelString(currentClass,
						currentMethod, "_while_label"));

		instructions.add(whileLabel);
		
		Object conditionValue = whileStatement.getCondition().accept(this);
		
		Register conditionReg;
		if (conditionValue instanceof List<?>) {
			List<LIRInstruction> insts = ((List<LIRInstruction>)conditionValue);
			Move moveInst = (Move)insts.get(insts.size() - 2);
			conditionReg = (Register)moveInst.getSecondOperand();
			instructions.addAll(insts);
		} else {
			if (conditionValue instanceof Register)
				conditionReg = (Register)conditionValue;
			else {
				conditionReg = RegisterPool.get(whileStatement);
				instructions.add(new Move(whileStatement,
						(BasicOperand)conditionValue, conditionReg));
			}
		}
		
		Label endLabel = LabelMaker.get(whileStatement,
				LabelMaker.labelString(currentClass,
						currentMethod, "_end_label"));
		
		instructions.add(new BinaryLogical(whileStatement, BinaryOps.Compare,
				new ConstantInteger(whileStatement, 0), conditionReg));
		instructions.add(new Jump(whileStatement, JumpOps.JumpTrue, endLabel));
		
		if (!(conditionValue instanceof Register))
			RegisterPool.putback(conditionReg);
		
		labelsToBreak.push(endLabel);
		labelsToContinue.push(whileLabel);
		
		instructions.addAll((List<LIRInstruction>)
				whileStatement.getOperation().accept(this));

		labelsToBreak.pop();
		labelsToContinue.pop();
		
		instructions.add(new Jump(whileStatement, JumpOps.Jump, whileLabel));
		instructions.add(endLabel);

		return instructions;
	}

	@Override
	public Object visit(Break breakStatement) {
		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();		
		instructions.add(labelsToBreak.peek());
		return instructions;
	}

	@Override
	public Object visit(Continue continueStatement) {
		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();		
		instructions.add(labelsToContinue.peek());
		return instructions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(StatementsBlock statementsBlock) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
				
		for (Statement stmt : statementsBlock.getStatements()) {
			instructions.addAll((List<LIRInstruction>)stmt.accept(this));
			returnRegistersToPool(stmt);
		}
		
		return instructions;

	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(LocalVariable localVariable) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		Symbol symbol = ScopesTraversal.findSymbol(
				localVariable.getName(), Kind.VARIABLE,
				localVariable.getEnclosingScope(),
				localVariable.getLine(),
				true /* variables declared only in method scopes */);
		
		//symbol != null because we passed semantic checks
		
		if (symbol.getLastStatementUsed() == null) {
			//variables is declared but never used
			return instructions;
		}
		
		if (localVariable.hasInitValue()) {
			
			Object initValue = localVariable.getInitValue().accept(this);
			
			if (initValue instanceof List<?>) {
				
				if (localVariable.getInitValue() instanceof NewClass) {
					
					//ugly up ahead...
					//(2nd instruction of newclass moves instance to register)
					
					Register reg =
					((RegisterOffset)((FieldStore)((List<LIRInstruction>)initValue)
							.get(1))
							.getSecondOperand()).getRegister();
					
					variables.put(symbol, reg);
					
					registerLastStatement.put(reg, symbol.getLastStatementUsed());
					if (symbol.getLastExpressionUsed() != null)
						registerLastExpression.put(reg, symbol.getLastExpressionUsed());
					
				} else if (localVariable.getInitValue() instanceof NewArray) {

					List<LIRInstruction> insts = (List<LIRInstruction>)initValue;
					Register reg = ((LibraryCall)insts.get(insts.size() - 1))
							.getReturnRegister();
					
					variables.put(symbol, reg);
					
					registerLastStatement.put(reg, symbol.getLastStatementUsed());
					if (symbol.getLastExpressionUsed() != null)
						registerLastExpression.put(reg, symbol.getLastExpressionUsed());
					
				}
				
				instructions.addAll((List<LIRInstruction>)initValue);
				
			} else {
				
				Register reg = RegisterPool.get(localVariable);
				
				instructions.add(new Move(localVariable,
						(BasicOperand)initValue,
						reg));
				
				variables.put(symbol, reg);
				
				registerLastStatement.put(reg, symbol.getLastStatementUsed());
				if (symbol.getLastExpressionUsed() != null)
					registerLastExpression.put(reg, symbol.getLastExpressionUsed());
			}
		}
		
		return instructions;
	}

	@Override
	public Object visit(VariableLocation location) {
		
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
				return variables.get(symbol);
			case FORMAL:
				return new Memory(location, symbol.getID());
			case FIELD:
				return new RegisterOffset(location, (Register)external,
						new ConstantInteger(location, dispatchTables.get(
								ScopesTraversal.getICClassFromClassScope(classScope))
							.getOffset((Field)symbol.getNode())));
										
			default:
				//never going to get here!
				return null;
		}

	}

	@Override
	public Object visit(ArrayLocation location) {

		BasicOperand array = (BasicOperand)location.getArray().accept(this);
		Object index = location.getIndex().accept(this);
		
		//TODO: when index is complex... (i.e. new int[max+2])
		
		return new LIR.Instructions.ArrayLocation(location, (Register)array, (BasicOperand)index);
	}

	@Override
	public Object visit(StaticCall call) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		//TODO: not library methods
		//TODO: not void return type
		
		if (call.getClassName().equals("Library")) {
			LibraryCall libCall = new LibraryCall(call,
					"__" + call.getName(),
					new Register(call, Register.DUMMY));
			
			for (Expression expr : call.getArguments()) {
				libCall.addParameter((BasicOperand)expr.accept(this));
			}
			
			instructions.add(libCall);
		}
		
		return instructions;
	}

	@Override
	public Object visit(VirtualCall call) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
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
		instructions.add(call);
		
		instructions.add(new FieldStore(newClass,
				new Memory(newClass, dispatchTable.getName()),
				new RegisterOffset(newClass, reg, new ConstantInteger(newClass, 0))
				)
			);
		
		return instructions;
	}

	@Override
	public Object visit(NewArray newArray) {
		
		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();

		Object val = newArray.getSize().accept(this);
				
		if (val instanceof ConstantInteger) {
			Register reg = RegisterPool.get(newArray);
			LibraryCall call = new LibraryCall(newArray,
					"__allocateArray", reg);
			call.addParameter(new ConstantInteger(
					newArray, 4 * ((ConstantInteger)val).getValue()));
			instructions.add(call);
		} else {
			Register sizeReg;
			if (!(val instanceof Register)) {
				sizeReg = RegisterPool.get(newArray);
				instructions.add(new Move(
						newArray, (BasicOperand)val, sizeReg));
			} else {
				sizeReg = (Register)val;
			}
			Register multReg = RegisterPool.get(newArray);
			
			//multiply size by 4 (for each element is 4 bytes):
			//sizeReg := sizeReg * 4
			instructions.add(new BinaryArithmetic(newArray,
					BinaryOps.Mul,
					new ConstantInteger(newArray, 4),
					sizeReg));
			
			RegisterPool.putback(multReg);
			Register reg = RegisterPool.get(newArray);

			LibraryCall call = new LibraryCall(newArray,
					"__allocateArray", reg);
			call.addParameter(sizeReg);
			instructions.add(call);
			if (!(val instanceof Register))
				RegisterPool.putback(sizeReg);
		}
		
		return instructions;
	}

	@Override
	public Object visit(Length length) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		//TODO: not necessary BasicOperand!
		
		BasicOperand op1 = (BasicOperand)binaryOp.getFirstOperand().accept(this);
		BasicOperand op2 = (BasicOperand)binaryOp.getSecondOperand().accept(this);
		
		Register result = RegisterPool.get(binaryOp);
		instructions.add(new Move(binaryOp, op2, result));
		
		//TODO: handle addition of strings (concatenation)
		
		BinaryOps op = null;
		if (binaryOp.getOperator() == IC.BinaryOps.PLUS) {
			op = BinaryOps.Add;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MINUS) {
			op = BinaryOps.Sub;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MULTIPLY) {
			op = BinaryOps.Mul;
		} else if (binaryOp.getOperator() == IC.BinaryOps.DIVIDE) {
			op = BinaryOps.Div;
		} else if (binaryOp.getOperator() == IC.BinaryOps.MOD) {
			op = BinaryOps.Mod;
		}
		
		instructions.add(new BinaryArithmetic(binaryOp, op, op1, result));
		
		return instructions;

	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {

		List<LIRInstruction> instructions = new LinkedList<LIRInstruction>();
		
		if (!binaryOp.getOperator().isLogicalOperation()) {
			
			//TODO: not necessary BasicOperand!

			BasicOperand op1 = (BasicOperand)binaryOp.getFirstOperand().accept(this);
			BasicOperand op2 = (BasicOperand)binaryOp.getSecondOperand().accept(this);
			
			Register result = RegisterPool.get(binaryOp);
			instructions.add(new Move(binaryOp, op2, result));			
			
			instructions.add(new BinaryLogical(
					binaryOp, BinaryOps.Compare, op1, result));
			
			IC.BinaryOps op = binaryOp.getOperator();
			JumpOps jump = null;
			if (op.isSizeComparisonOperation()) {
				if (op == IC.BinaryOps.LT) {
					jump = JumpOps.JumpLT;
				} else if (op == IC.BinaryOps.LTE){
					jump = JumpOps.JumpLE;
				} else if (op == IC.BinaryOps.GT){
					jump = JumpOps.JumpGT;
				} else if (op == IC.BinaryOps.GTE){
					jump = JumpOps.JumpGE;
				}
			} else if (op.isEqualityOperation()) {
				if (op == IC.BinaryOps.EQUAL) {
					jump = JumpOps.JumpFalse; //dst-src == 0 when dst=src; 0 is false
				} else if (op == IC.BinaryOps.NEQUAL) {
					jump = JumpOps.JumpTrue; //!(dst-src == 0)
				}
			}
			
			Label conditionHoldsLabel = LabelMaker.get(binaryOp,
					LabelMaker.labelString(currentClass, currentMethod, "_jump_label"));
			Label endLabel = LabelMaker.get(binaryOp,
					LabelMaker.labelString(currentClass, currentMethod, "_end_jump_label"));
			
			instructions.add(new Jump(binaryOp, jump, conditionHoldsLabel));
			instructions.add(new Move(binaryOp, new ConstantInteger(binaryOp, 0), result));
			instructions.add(new Jump(binaryOp, jump, endLabel));
			instructions.add(conditionHoldsLabel);
			instructions.add(new Move(binaryOp, new ConstantInteger(binaryOp, 1), result));
			instructions.add(endLabel);
			
		} else {
			
			BasicOperand op1 = (BasicOperand)binaryOp.getFirstOperand().accept(this);
			Register reg1;
			if (op1 instanceof Register)
				reg1 = (Register)op1;
			else {
				reg1 = RegisterPool.get(binaryOp);
				instructions.add(new Move(binaryOp, op1, reg1));
			}
			
			instructions.add(new BinaryLogical(binaryOp,
					BinaryOps.Compare,
					new ConstantInteger(binaryOp,
							binaryOp.getOperator() == IC.BinaryOps.LOR ? 1 : 0),
					reg1));
			
			Label endLabel = LabelMaker.get(binaryOp,
					LabelMaker.labelString(currentClass, currentMethod, "_end_label"));
			
			instructions.add(new Jump(binaryOp, JumpOps.JumpTrue, endLabel));

			BasicOperand op2 = (BasicOperand)binaryOp.getSecondOperand().accept(this);
			Register reg2;
			if (op2 instanceof Register)
				reg2 = (Register)op2;
			else {
				reg2 = RegisterPool.get(binaryOp);
				instructions.add(new Move(binaryOp, op2, reg2));
			}
			
			instructions.add(new BinaryLogical(binaryOp,
					(binaryOp.getOperator() == IC.BinaryOps.LOR
						? BinaryOps.Or : BinaryOps.And),
					reg2, reg1));
			instructions.add(endLabel);
			
			RegisterPool.putback(reg2);
			
		}

		return instructions;
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
			break;
		case INTEGER:
			return new ConstantInteger(literal,
					Integer.valueOf(literal.getValue() + ""));
		case NULL:
			//TODO: use _checkNullRef()
			return new ConstantNull(literal);
		case STRING:
			return new Memory(literal,
					literals.get(literal.getValue() + "").getId());
		case TRUE:
			break;
		}
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		// TODO Auto-generated method stub.
		return null;
	}

}
