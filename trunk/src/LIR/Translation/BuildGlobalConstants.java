package LIR.Translation;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IC.LiteralTypes;
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

public class BuildGlobalConstants implements Visitor {

	/* This class scans the AST and builds the String Literal
	 * Set and the Dispatch Table for each class.
	 */

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;
	
	private ICClass currentClass;
	
	public BuildGlobalConstants() {
		this.literals = new StringLiteralSet();
		this.dispatchTables = new HashMap<ICClass, DispatchTable>();
	}
	
	@Override
	public Object visit(Program program) {
		
		//first sort classes so that the non-inheriting ones will
		//be checked first (and therefore when we reach their subclasses,
		//their dispatch tables would have been defined by then.
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
		
		for (ICClass cls : classes) {
			cls.accept(this);
		}
		
		return new GlobalConstantsWrapper(literals, dispatchTables);
	}

	@Override
	public Object visit(ICClass icClass) {
		
		if (!dispatchTables.containsKey(icClass)) {
			DispatchTable dt = new DispatchTable(icClass.getName());
			if (icClass.hasSuperClass()) {
				dt = (DispatchTable)dispatchTables.get(
						currentClass).clone();
			}
			dispatchTables.put(icClass, dt);
		}
		
		currentClass = icClass;
		
		for (Field field : icClass.getFields()) {
			field.accept(this);
		}

		for (Method method : icClass.getMethods()) {
			method.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(Field field) {
		dispatchTables.get(currentClass).addField(field);
		return null;
	}

	@Override
	public Object visit(VirtualMethod method) {
		dispatchTables.get(currentClass).addMethod(method);
		
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		//dispatch table is only for virtual methods
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}		
		return null;
	}

	@Override
	public Object visit(LibraryMethod method) {
		// do nothing (no statements)
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

	@Override
	public Object visit(Assignment assignment) {
		assignment.getAssignment().accept(this);
		assignment.getVariable().accept(this);
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		callStatement.getCall().accept(this);
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		if (returnStatement.hasValue())
			returnStatement.getValue().accept(this);
		return null;
	}

	@Override
	public Object visit(If ifStatement) {
		ifStatement.getCondition().accept(this);
		ifStatement.getOperation().accept(this);
		if (ifStatement.hasElse())
			ifStatement.getElseOperation().accept(this);
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		whileStatement.getCondition().accept(this);
		whileStatement.getOperation().accept(this);
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) {
		for (Statement stmt : statementsBlock.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(LocalVariable localVariable) {
		if (localVariable.hasInitValue())
			localVariable.getInitValue().accept(this);
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		if (location.isExternal())
			location.getLocation().accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		location.getArray().accept(this);
		location.getIndex().accept(this);
		return null;
	}

	@Override
	public Object visit(StaticCall call) {
		for (Expression expr : call.getArguments()) {
			expr.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {

		if (call.isExternal())
			call.getLocation().accept(this);
		
		for (Expression expr : call.getArguments()) {
			expr.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		// do nothing
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		newArray.getSize().accept(this);
		return null;
	}

	@Override
	public Object visit(Length length) {
		length.getArray().accept(this);
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		binaryOp.getFirstOperand().accept(this);
		binaryOp.getSecondOperand().accept(this);
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		binaryOp.getFirstOperand().accept(this);
		binaryOp.getSecondOperand().accept(this);
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		unaryOp.getOperand().accept(this);
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		unaryOp.getOperand().accept(this);
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		if (literal.getType() == LiteralTypes.STRING)
			literals.add(literal);
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		expressionBlock.getExpression().accept(this);
		return null;
	}
	
}
