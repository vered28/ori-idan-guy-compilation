package IC.Semantics.Validations;

import java.util.HashMap;
import java.util.Map;

import IC.BinaryOps;
import IC.DataTypes;
import IC.UnaryOps;
import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.CallStatement;
import IC.AST.Continue;
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
import IC.Semantics.SemanticError;
import IC.Semantics.TypeMismatchSemanticError;
import IC.Semantics.Scopes.Type;

public class TypesValidation implements Visitor {

	private Map<String, Type> typeTable;
	private ICClass curClass;
	private Method curMethod;

	public TypesValidation() {
		typeTable = new HashMap<>();
		addPrimitiveTypesToTypeTable();
	}

	@Override
	public Object visit(Program program) {
		addUserTypesToTypeTable(program);
		for (ICClass icClass : program.getClasses()) {
			curClass = icClass;
			icClass.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {
		for (Method method : icClass.getMethods()) {
			curMethod = method;
			method.accept(this);
		}
		icClass.setNodeType(typeTable.get(icClass.getName()));
		return null;
	}

	@Override
	public Object visit(Field field) {
		field.getType().accept(this);
		field.setNodeType(typeTable.get(field.getType().getName()));
		return null;
	}

	@Override
	public Object visit(VirtualMethod method) {
		return visitMethod(method);
	}

	@Override
	public Object visit(StaticMethod method) {
		return visitMethod(method);
	}

	@Override
	public Object visit(LibraryMethod method) {
		return visitMethod(method);
	}

	@Override
	public Object visit(Formal formal) {
		formal.getType().accept(this);
		formal.setNodeType(typeTable.get(formal.getType().getName()));
		return null;
	}

	@Override
	public Object visit(PrimitiveType type) {
		type.setNodeType(
				typeTable.get(type.getPrimitiveType().getDescription()));
		return null;
	}

	@Override
	public Object visit(UserType type) {
		type.setNodeType(typeTable.get(type.getName()));
		return null;
	}

	@Override
	public Object visit(Assignment assignment) {
		assignment.getVariable().accept(this);
		assignment.getAssignment().accept(this);

		if (assignment.getAssignment().getNodeType().subTypeOf(
				assignment.getVariable().getNodeType())) {
			assignment.setNodeType(assignment.getVariable().getNodeType());
		}
		else {
			throw new TypeMismatchSemanticError(
					assignment.getAssignment().getNodeType(),
					assignment.getVariable().getNodeType(),
					assignment.getAssignment());
		}
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		callStatement.getCall().accept(this);
		callStatement.setNodeType(callStatement.getCall().getNodeType());
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		Type methodReturnType =
				curMethod.getEnclosingScope().getParentScope().getSymbol(
						curMethod.getName()).getType();
		if (returnStatement.hasValue()) {
			returnStatement.getValue().accept(this);

			if (methodReturnType.subTypeOf(
					returnStatement.getValue().getNodeType())) {
				returnStatement.setNodeType(
						returnStatement.getValue().getNodeType());
			}
			else {
				throw new TypeMismatchSemanticError(
						returnStatement.getValue().getNodeType(),
						methodReturnType, returnStatement.getValue());
			}
		}
		else {
			if (methodReturnType.isVoid()) {
				returnStatement.setNodeType(
						typeTable.get(DataTypes.VOID.getDescription()));
			}
		}
		return null;
	}

	@Override
	public Object visit(If ifStatement) {
		ifStatement.getCondition().accept(this);
		ifStatement.getOperation().accept(this);
		
		if (ifStatement.hasElse()) {
			ifStatement.getElseOperation().accept(this);
		}
		if (ifStatement.getCondition().getNodeType().isBoolean()) {
			ifStatement.setNodeType(typeTable.get(DataTypes.BOOLEAN.getDescription()));
		}
		else {
			throw new SemanticError("The condition part of an 'if' " +
					"statement should be boolean.",
					ifStatement.getCondition());
		}
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		whileStatement.getCondition().accept(this);
		whileStatement.getOperation().accept(this);
		if (!whileStatement.getCondition().getNodeType().isBoolean()) {
			throw new SemanticError("The condition part of an 'while' " +
					"statement should be boolean.",
					whileStatement.getCondition());
		}
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		// TODO Auto-generated method stub
		// Do nothing
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		// TODO Auto-generated method stub
		// Do nothing
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
		localVariable.getType().accept(this);
		localVariable.setNodeType(
				localVariable.getEnclosingScope().getSymbol(
						localVariable.getName()).getType());
		if (localVariable.hasInitValue()) {
			localVariable.getInitValue().accept(this);
			if (!localVariable.getInitValue().getNodeType().subTypeOf(
					localVariable.getType().getNodeType())) {
				throw new TypeMismatchSemanticError(
						localVariable.getInitValue().getNodeType(),
						localVariable.getType().getNodeType(),
						localVariable.getInitValue());
			}
		}
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		if (location.isExternal()) {
			location.getLocation().accept(this);
			Type type = typeTable.get(
					location.getLocation().getNodeType().getName());
			if (type instanceof IC.Semantics.Scopes.UserType) {
				ICClass icClass = ((IC.Semantics.Scopes.UserType)type).getClassNode();
				String typeName = icClass.getField(location.getName()).
						getType().getName();
				location.setNodeType(typeTable.get(typeName));
			}
			else {
				throw new SemanticError("Cannot make a reference from a " + 
						"primitive variable.", location.getLocation());
			}
		}
		else {
			location.setNodeType(location.getEnclosingScope().getSymbol(
					location.getName()).getType());
		}
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		location.getArray().accept(this);
		location.getIndex().accept(this);
		if (location.getArray().getNodeType().getDimension() >= 1) {
			if (location.getIndex().getNodeType().isInteger()) {
				location.setNodeType(location.getArray().getNodeType());
			}
			else {
				throw new SemanticError("Array index should be in integer.",
						location.getIndex());
			}
		}
		else {
			throw new SemanticError(
					"Invalid use of scope resolution on a non array type.",
					location.getIndex());
		}
		return null;
	}

	@Override
	public Object visit(StaticCall call) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		thisExpression.setNodeType(typeTable.get(curClass.getName()));
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		newClass.setNodeType(typeTable.get(newClass.getName()));
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		newArray.getType().accept(this);
		newArray.getSize().accept(this);
		if (newArray.getSize().getNodeType().isInteger()) {
			// TODO Auto-generated method stub
			//			for (int i = 1; i <= newArray.getType().getDimension(); i++) {
			//				typeTable.put(bla bla bla...)
			//			}
			newArray.setNodeType(typeTable.get(newArray.getType().getName()));
		}
		else {
			throw new SemanticError(
					"The array size must be an integer.", newArray.getSize());
		}
		return null;
	}

	@Override
	public Object visit(Length length) {
		length.getArray().accept(this);
		if (length.getArray().getNodeType().getDimension() > 0) {
			length.setNodeType(typeTable.get(DataTypes.INT.getDescription()));
		}
		else {
			throw new SemanticError(
					"'length' can be used only on array types.", length);
		}
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		binaryOp.getFirstOperand().accept(this);
		binaryOp.getSecondOperand().accept(this);

		if (binaryOp.getOperator().isMathOperation()) {
			if (binaryOp.getFirstOperand().getNodeType().isString() &&
					binaryOp.getSecondOperand().getNodeType().isString()) {
				if (binaryOp.getOperator() == BinaryOps.PLUS) {
					binaryOp.setNodeType(typeTable.get(DataTypes.STRING.getDescription()));
				}
				else {
					throw new SemanticError(
							"Invalid operation on two String variables.", binaryOp);
				}
			}
			else if (binaryOp.getFirstOperand().getNodeType().isInteger() &&
					binaryOp.getSecondOperand().getNodeType().isInteger()) {
				binaryOp.setNodeType(typeTable.get(DataTypes.INT.getDescription()));
			}
			else {
				throw new SemanticError("Invalid operation on two int variables.", binaryOp);
			}
		}
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		binaryOp.getFirstOperand().accept(this);
		binaryOp.getSecondOperand().accept(this);

		if (binaryOp.getOperator().isEqualityOperation()) {
			if (binaryOp.getFirstOperand().getNodeType().subTypeOf(
					binaryOp.getSecondOperand().getNodeType()) ||
					binaryOp.getSecondOperand().getNodeType().subTypeOf(
							binaryOp.getFirstOperand().getNodeType())) {
				binaryOp.setNodeType(typeTable.get(DataTypes.BOOLEAN.getDescription()));
			}
			else {
				throw new SemanticError("Invalid usage of comarison " +
						"operation (can only be used on types A and B " +
						"such that A extends B or B extend A).", binaryOp);
			}
		}
		else if (binaryOp.getOperator().isSizeComparisonOperation()) {
			if (binaryOp.getFirstOperand().getNodeType().isInteger() &&
					binaryOp.getSecondOperand().getNodeType().isInteger()) {
				binaryOp.setNodeType(typeTable.get(DataTypes.BOOLEAN.getDescription()));
			}
			else {
				throw new SemanticError("Mathematical binary operator can be " +
						"used only on two integers.", binaryOp);
			}
		}
		else if (binaryOp.getOperator().isLogicalOperation()) {
			if (binaryOp.getFirstOperand().getNodeType().isBoolean() &&
					binaryOp.getSecondOperand().getNodeType().isBoolean()) {
				binaryOp.setNodeType(typeTable.get(DataTypes.BOOLEAN.getDescription()));
			}
			else {
				throw new SemanticError("A logical binary operator can be " +
						"used only on two booleans.", binaryOp);
			}
		}
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		unaryOp.getOperand().accept(this);

		if (unaryOp.getOperator() != UnaryOps.UMINUS) {
			throw new SemanticError(
					"Invalid unary mathematical operation.", unaryOp);
		}
		if (!unaryOp.getOperand().getNodeType().isInteger()) {
			throw new SemanticError(
					"An unary mathematical operation can be used only on " +
							"integers.", unaryOp);
		}
		unaryOp.setNodeType(typeTable.get(DataTypes.INT.getDescription()));
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		unaryOp.getOperand().accept(this);

		if (unaryOp.getOperator() != UnaryOps.LNEG) {
			throw new SemanticError(
					"Invalid unary logical operation.", unaryOp);
		}
		if (!unaryOp.getOperand().getNodeType().isBoolean()) {
			throw new SemanticError(
					"An unary logical operation can be used only on " +
							"booleans.", unaryOp);
		}
		unaryOp.setNodeType(typeTable.get(DataTypes.BOOLEAN.getDescription()));
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		Type type = null;
		switch (literal.getType()) {
		case INTEGER:
			type = typeTable.get(DataTypes.INT.getDescription());
			break;
		case STRING:
			type = typeTable.get(DataTypes.STRING.getDescription());
			break;
		case TRUE:
		case FALSE:
			type = typeTable.get(DataTypes.BOOLEAN.getDescription());
			break;
		case NULL:
			// TODO Auto-generated method stub ---> null (nothing to do?)
			// add null to DataTypes
			break;

		default:
			break;
		}
		literal.setNodeType(type);
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		expressionBlock.getExpression().accept(this);
		expressionBlock.setNodeType(
				expressionBlock.getExpression().getNodeType());
		return null;
	}

	private void addUserTypesToTypeTable(Program program) {
		for (ICClass icClass : program.getClasses()) {
			typeTable.put(icClass.getName(),
					new IC.Semantics.Scopes.UserType(
							icClass.getName(), icClass));
		}
	}

	private void addPrimitiveTypesToTypeTable() {
		for (DataTypes dataType : DataTypes.values()) {
			typeTable.put(dataType.getDescription(),
					new IC.Semantics.Scopes.PrimitiveType(dataType));
		}
	}

	private Object visitMethod(Method method) {
		method.getType().accept(this);
		for (Formal formal : method.getFormals()) {
			formal.accept(this);
		}
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		method.setNodeType(
				method.getEnclosingScope().getParentScope().getSymbol(
						method.getName()).getType());
		return null;
	}

}
