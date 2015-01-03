package IC.Semantics.Validations;

import java.util.Arrays;
import java.util.List;

import IC.BinaryOps;
import IC.DataTypes;
import IC.AST.ASTNode;
import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.Call;
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
import IC.Semantics.Exceptions.SemanticError;
import IC.Semantics.Exceptions.TypeMismatchSemanticError;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.Symbol;
import IC.Semantics.Types.ClassTypeEntry;
import IC.Semantics.Types.PrimitiveTypeEntry;
import IC.Semantics.Types.Type;
import IC.Semantics.Types.TypeTable;

public class TypesValidation implements Visitor {

	private TypeTable typeTable;
	
	private Method currentMethod;

	public TypesValidation(TypeTable typeTable) {
		this.typeTable = typeTable;
	}
	
	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine) {
		
		Symbol symbol;
		for (Kind k : kinds) {
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine)) != null)
				return symbol;
		}

		return null;
	}
	
	/**
	 * checks if t2 is sub-type of t1, but they are not array (if arrays,
	 * checks that they are of equal dimension and that they are exactly
	 * the same type).
	 */
	private void checkSubtypesNotArrays(Type t1, Type t2, ASTNode node, String errMsg) {
		
		if (!t2.subTypeOf(t1)) {
			throw new TypeMismatchSemanticError(
					t2, t1, node);
		}
		
		//if arrays, check they are exactly the same type and match
		//in dimensions:
		if (t1.getDimension() > 0 || t2.getDimension() > 0) {
			
			if (t1.getDimension() != t2.getDimension()) {
				throw new SemanticError(errMsg, node);
			}
			
			//if A extends B, cannot assign B[] to A[]
			if (!t1.equals(t2)) {
				throw new TypeMismatchSemanticError(t2, t1, node);
			}
			
		}
	}
	
	private boolean isPrimitive(IC.Semantics.Types.Type type) {
		return (type instanceof IC.Semantics.Types.PrimitiveType);
	}
	
	private boolean isBoolean(IC.Semantics.Types.Type type) {
		return isPrimitive(type) &&
				((IC.Semantics.Types.PrimitiveType)type).isBoolean();
	}

	private boolean isInteger(IC.Semantics.Types.Type type) {
		return isPrimitive(type) &&
				((IC.Semantics.Types.PrimitiveType)type).isInteger();
	}

	private boolean isString(IC.Semantics.Types.Type type) {
		return isPrimitive(type) &&
				((IC.Semantics.Types.PrimitiveType)type).isString();
	}

	private boolean isVoid(IC.Semantics.Types.Type type) {
		return isPrimitive(type) &&
				((IC.Semantics.Types.PrimitiveType)type).isVoid();
	}

	@Override
	public Object visit(Program program) {
				
		for (ICClass icClass : program.getClasses()) {
			icClass.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {
		
		//go over all fields so you can assign their type
		//to their nodes.
		for (Field field : icClass.getFields()) {
			field.accept(this);
		}
		
		//next go over all methods and check their types (assign
		//return type to their nodes):
		for (Method method : icClass.getMethods()) {
			currentMethod = method;
			method.accept(this);
		}
		
		//everything okay with children nodes -- set type to this node:
		//icClass.setNodeType(typeTable.get(icClass.getName()));
		
		return null;
	}

	@Override
	public Object visit(Field field) {
		
		//set type to node (by visiting type):
		field.setNodeType((Type)field.getType().accept(this));
		
		//nothing to return
		return null;
	}
	
	private void checkLegalOverride(Method method, boolean isStatic) {
		
		ClassScope scope = (ClassScope)method.getEnclosingScope().getParentScope();
		if (scope.getParentScope() instanceof ClassScope) {
			
			//method is defined within a derive class, it may be an override.
			//an override is only legal if it does not change the original
			//method's signature. We'll validated same number of formals,
			//now let's validate types weren't changed.
			
			Symbol originalMethod = ScopesTraversal.findSymbol(method.getName(),
					isStatic ? Kind.STATICMETHOD : Kind.VIRTUALMETHOD,
					scope.getParentScope());
			
			if (originalMethod != null) {
				
				//number of formals is the same (validated already)
				
				List<Formal> originalFormals =
						((Method)originalMethod.getNode()).getFormals();
				List<Formal> formals = method.getFormals();
				
				int size = formals.size();
				for (int i = 0; i < size; i++) {
					Type originalType = (Type)originalFormals.get(i).accept(this);
					Type type = (Type)formals.get(i).accept(this);
					checkSubtypesNotArrays(originalType, type, method,
							"Mismatch array dimensions in overriding formal " +
							(isStatic ? "static" : "virtual") + " method '"
							+ method.getName() + "' in class " + scope.getID());
				}
				
				//lastly, compare return types:
				Type originalReturnType = (Type)((Method)originalMethod.getNode()).getType().accept(this);
				Type returnType = (Type)method.getType().accept(this);
				checkSubtypesNotArrays(originalReturnType, returnType, method,
						"Mismatch array dimensions in return value of overriden " +
						(isStatic ? "static" : "virtual") + " method '"
						+ method.getName() + "' in class " + scope.getID());

				
			}
		}
	}

	private Object visitMethod(Method method, boolean isStatic) {
		
		checkLegalOverride(method, isStatic);
		
		Type returnType = (Type)method.getType().accept(this);
		
		for (Formal formal : method.getFormals()) {
			formal.accept(this);
		}
		
		currentMethod = method;
		
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
				
		method.setNodeType(returnType);
		
		return null;
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
		return visitMethod(method, true);
	}

	@Override
	public Object visit(Formal formal) {
		
		Type formalType = (Type)formal.getType().accept(this);
		
		//set type to node (by visiting type):
		formal.setNodeType(formalType);
		
		return formalType;
	}

	@Override
	public Object visit(PrimitiveType type) {
		
		Type semanticType = ((PrimitiveTypeEntry)typeTable.get(type.getName()))
								.getPrimitiveType();
		
		Type resultType = semanticType.clone();
		resultType.setDimension(type.getDimension());

		type.setNodeType(resultType);
		return resultType;
	}

	@Override
	public Object visit(UserType type) {
		Type semanticType = ((ClassTypeEntry)typeTable.get(type.getName()))
				.getUserType();
				
		Type resultType = semanticType.clone();
		resultType.setDimension(type.getDimension());
		
		type.setNodeType(resultType);
		return resultType;
	}

	@Override
	public Object visit(Assignment assignment) {
		
		Type variableType = (Type)assignment.getVariable().accept(this);
		Type assignmentType = (Type)assignment.getAssignment().accept(this);

		//rule is: var = assign iff var <= assign
		//         (var and assign of same type or assign type extends var type)
		checkSubtypesNotArrays(variableType, assignmentType,
				assignment, "Array dimensions mismatch on assignment.");
				
		return variableType;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		Type type = (Type)callStatement.getCall().accept(this);
		callStatement.setNodeType(type);
		return type;
	}

	@Override
	public Object visit(Return returnStatement) {
		
		Type expectedMethodReturnType =
				currentMethod.getEnclosingScope().getParentScope().getSymbol(
						currentMethod.getName()).getType();
		
		if (returnStatement.hasValue()) {
			Type returnType = (Type)returnStatement.getValue().accept(this);
			checkSubtypesNotArrays(expectedMethodReturnType, returnType,
					returnStatement, "Array dimensions mismatch between method return "+
							"type and return statement.");
		} else {
			if (!isVoid(expectedMethodReturnType)) {
				throw new SemanticError("Void return value for non-void method.",
						returnStatement.getLine(),
						returnStatement.getColumn());
			}
		}
		
		returnStatement.setNodeType(expectedMethodReturnType);
		
		return expectedMethodReturnType;
	}

	@Override
	public Object visit(If ifStatement) {
		
		Type conditionType = (Type)ifStatement.getCondition().accept(this);
		
		if (!isBoolean(conditionType)) {
			throw new SemanticError("The condition part of an 'if' " +
					"statement must be resolved to a boolean type.",
					ifStatement.getCondition());
		}
		
		//condition is okay (type boolean), visit operations:
		
		ifStatement.getOperation().accept(this);
		if (ifStatement.hasElse()) {
			ifStatement.getElseOperation().accept(this);
		}
		
		//nothing to return or set as the node type (keep it null)
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		
		Type conditionType = (Type)whileStatement.getCondition().accept(this);
		
		if (!isBoolean(conditionType)) {
			throw new SemanticError("The condition part of a 'while' " +
					"statement must be resolved to a boolean type.",
					whileStatement.getCondition());			
		}
		
		//condition is okay, visit operation:
		whileStatement.getOperation().accept(this);

		//nothing to return or set as the node type (keep it null)
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		//nothing to return or set as the node type (keep it null)
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		///nothing to return or set as the node type (keep it null)
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
		
		Type type = (Type)localVariable.getType().accept(this);
		
		if (localVariable.hasInitValue()) {
			
			Type valueType = (Type)localVariable.getInitValue().accept(this);
			
			//rule is: type and value type must either match or value type
			//         must be a subclass of type.
			checkSubtypesNotArrays(type, valueType, localVariable,
					"Mismatch on array dimensions when assigning "+
							"initial value to local variable.");

		}
		
		localVariable.setNodeType(type);

		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		
		Type locationType = null;
		
		if (location.isExternal()) {
			
			Type type = (Type)location.getLocation().accept(this);
			
			//must be usertype (i.e. can't do 12.something and so on)
			//DeclarationValidation already throws error if not
			
			//for "this" keyword, we already return the class type --> the lookup
			//will only take one step --> we'll get the class scope as required.
			ClassScope scope = (ClassScope)ScopesTraversal.getClassScopeByName(
					location.getEnclosingScope(), type.getName());
			
			//find type of location.getName() in class scope:
			//(cannot get ambiguity, this is variable location, not static/virtual call!)
			
			Symbol symbol = findSymbol(location.getName(),
					Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
					scope, location.getLine());
			
			locationType = symbol.getType();
			location.setNodeType(locationType);
			
		} else {
			
			Symbol symbol = findSymbol(location.getName(),
					Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
					location.getEnclosingScope(),
					location.getLine());
			locationType = symbol.getType();
			location.setNodeType(locationType);
			
		}
		
		return locationType;
	}

	@Override
	public Object visit(ArrayLocation location) {
		
		Type arrayType = (Type)location.getArray().accept(this);
		Type indexType = (Type)location.getIndex().accept(this);
		
		if (arrayType.getDimension() == 0) {
			throw new SemanticError(
					"Invalid use of scope resolution on non array type.",
					location.getIndex());
		}
		
		if (!isInteger(indexType)) {
			throw new SemanticError("Array index must be resolved to an integer value.",
					location.getIndex());
		}
		
		//if building a new array, increment dimension. Otherwise, decrement dimension:
		//why? if new int[x][y][z], then we're building another dimension.
		//     if arr[x][y][z] we have a 3-dimensional array and are returning a value that is a 2-dimensional array
		boolean newArray = false;
		ArrayLocation loc = location;
		while (loc != null) {
			if (loc.getArray() instanceof NewArray) {
				newArray = true;
				break;
			} else if (loc.getArray() instanceof ArrayLocation) {
				loc = (ArrayLocation)loc.getArray();
			} else {
				break;
			}
		}
		
		Type resultType = arrayType.clone();
		if (newArray) {
			resultType.setDimension(arrayType.getDimension() + 1);
			resultType.setName(resultType.getName() + "[]");
		} else {
			resultType.setDimension(arrayType.getDimension() - 1);
			if (resultType.getName().endsWith("[]")) {
				resultType.setName(
						resultType.getName().substring(0,
								resultType.getName().length() -2));
			}
		}
		
		location.setNodeType(resultType);
		
		return resultType;
	}
	
	/**
	 * Checks arguments and formals match between call and method.
	 *
	 * @return method's return type
	 */
	public Object visitCall(Call call, Symbol methodSymbol) {
	
		int size = call.getArguments().size();
		Method method = (Method)methodSymbol.getNode();

		for (int i = 0; i < size; i++) {
			Type formalType = (Type)method.getFormals().get(i).accept(this);
			Type argumentType = (Type)call.getArguments().get(i).accept(this);
			checkSubtypesNotArrays(formalType, argumentType, call,
					"Mismatch array dimensions between formal '"
							+ method.getFormals().get(i).getName()
							+ "' and argument at " 
							+ (methodSymbol.getKind().equals(Kind.STATICMETHOD) ? "static" : "virtual")
							+ " method call");
		}
		
		call.setNodeType(methodSymbol.getType());
		
		//return method return type
		return methodSymbol.getType();
		
	}

	@Override
	public Object visit(StaticCall call) {

		//we've already validated method exists in correct scope,
		//with the right number of arguments. All that's left
		//is to check that the arguments of the call match the
		//method formals (type-wise)
		
		ClassScope scope = (ClassScope)
				ScopesTraversal.getClassScopeByName(
						call.getEnclosingScope(),
						call.getClassName());
		
		Symbol methodSymbol = ScopesTraversal.findSymbol(
				call.getName(), Kind.STATICMETHOD, scope);
		
		return visitCall(call, methodSymbol);

	}

	@Override
	public Object visit(VirtualCall call) {

		//we've already validated method exists in correct scope,
		//with the right number of arguments. All that's left
		//is to check that the arguments of the call match the
		//method formals (type-wise)

		//the catch for virtual call -- it may also be a static
		//call to a static method in the same class scope.
		
		Type external = null;
		boolean virtualMethod = true;
		
		if (call.isExternal()) {
			
			external = (Type)call.getLocation().accept(this);
			//visit(thisExpression) return the this instance (class scope)
			//as its' type, so it handles that as well
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
		
		Symbol methodSymbol = ScopesTraversal.findSymbol(
				call.getName(),
				virtualMethod ? Kind.VIRTUALMETHOD : Kind.STATICMETHOD,
				(external == null) ? call.getEnclosingScope() : 
					ScopesTraversal.getClassScopeByName(call.getEnclosingScope(), external.getName())
				);

		return visitCall(call, methodSymbol);
		
	}

	@Override
	public Object visit(This thisExpression) {
		
		//get type of class we're running in (class scope parent of current scope):
		Type thisType = ((ClassTypeEntry)typeTable.get(
							ScopesTraversal.getClassScopeOfCurrentScope(
											thisExpression.getEnclosingScope())
										.getID())).getUserType();
		
		thisExpression.setNodeType(thisType);
		return thisType;
	}

	@Override
	public Object visit(NewClass newClass) {
		
		//get type of class name
		//(we've already checked it exists in DeclarationValidation):
		Type newClassType = ((ClassTypeEntry)typeTable.get(newClass.getName())).getUserType();
		
		newClass.setNodeType(newClassType);
		return newClassType;
		
	}

	@Override
	public Object visit(NewArray newArray) {
		
		Type arrayType = (Type)newArray.getType().accept(this);
		Type sizeType = (Type)newArray.getSize().accept(this);
		
		if (!isInteger(sizeType)) {
			throw new SemanticError(
					"Array size must be resolved to an integer.",
					newArray.getSize());
		}

		Type resultType = arrayType.clone();
		resultType.setDimension(arrayType.getDimension() + 1);
		resultType.setName(resultType.getName() + "[]");
		
		newArray.setNodeType(resultType);

		return resultType;
	}

	@Override
	public Object visit(Length length) {
		
		Type arrayType = (Type)length.getArray().accept(this);
		
		if (arrayType.getDimension() == 0) {
			throw new SemanticError(
					"'length' is only applicable for arrays.", length);
		}
		
		Type intType = ((PrimitiveTypeEntry)typeTable.get(
								DataTypes.INT.getDescription()))
						.getPrimitiveType();
		length.setNodeType(intType);
		
		return intType;
		
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		
		Type firstType = (Type)binaryOp.getFirstOperand().accept(this);
		Type secondType = (Type)binaryOp.getSecondOperand().accept(this);
		
		Type resultType = null;

		if (isString(firstType) && isString(secondType)) {
			//only addition is allowed for strings (concatenation):
			if (binaryOp.getOperator() != BinaryOps.PLUS) {
				throw new SemanticError(
						"Illegal binary operation "
								+ binaryOp.getOperator().getDescription()
								+ " on strings.",
						binaryOp);
			}

			resultType = ((PrimitiveTypeEntry)typeTable.get(
					DataTypes.STRING.getDescription())).getPrimitiveType();

			
		} else if (isInteger(firstType) && isInteger(secondType)) {
			//all math binary operations allowed on integer:
			resultType = ((PrimitiveTypeEntry)typeTable.get(
					DataTypes.INT.getDescription())).getPrimitiveType();
		} else {
			throw new SemanticError("Illegal binary operation " +
										binaryOp.getOperator().getDescription()
										+ " between " + firstType.toString() +
										" and " + secondType.toString() + ".",
									binaryOp);
		}

		binaryOp.setNodeType(resultType);
		return resultType;
		
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		
		Type firstType = (Type)binaryOp.getFirstOperand().accept(this);
		Type secondType = (Type)binaryOp.getSecondOperand().accept(this);
		
		if (binaryOp.getOperator().isEqualityOperation()) {
			
			//binaryOp is either == or !=
			
			//rule is we can only compare between two expressions of
			//same type (or that one derives from the other):
			if (!firstType.subTypeOf(secondType) &&
					!secondType.subTypeOf(firstType)) {
				throw new SemanticError("Invalid usage of comparison " +
						"operation (can only compare between two expressions"
						+ " of same type, or that one extends the other.)",
						binaryOp);
			}

		} else if (binaryOp.getOperator().isSizeComparisonOperation()) {
			
			//binaryOp is either <=, <, > or >=
			
			//rule is we can only compare between integers
			
			if (!isInteger(firstType) || !isInteger(secondType)) {
				throw new SemanticError("Can only compare between integer.",
						binaryOp);
			}

		} else if (binaryOp.getOperator().isLogicalOperation()) {
			
			//binaryOp is either && or ||
			
			//rule is we can only perform logical binary operations on booleans:
			
			if (!isBoolean(firstType) || !isBoolean(secondType)) {
				throw new SemanticError("A logical binary operator can be " +
						"used only on two booleans.", binaryOp);
			}
		
		}
		
		//we've passed all inspections without throwing an exception,
		//everything is okay and the result is a boolean type:
		
		Type resultType = ((PrimitiveTypeEntry)typeTable.get(
				DataTypes.BOOLEAN.getDescription())).getPrimitiveType();
		binaryOp.setNodeType(resultType);
		return resultType;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		
		//unaryOp is - (negation, i.e turn 4 to -4 and -2 to --2=2):

		Type type = (Type)unaryOp.getOperand().accept(this);

		//rule is operator is only applied to integers:
		
		if (!isInteger(type)) {
			throw new SemanticError(
					"An unary mathematical operation can be used only on " +
							"integers.", unaryOp);
		}
		
		unaryOp.setNodeType(type); //since we know type is integer :)
		return type;
		
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		
		//unaryOp is ! (negation, i.e turn false to true and vice versa):

		Type type = (Type)unaryOp.getOperand().accept(this);

		//rule is we can only apply operator to boolean
		
		if (!isBoolean(type)) {
			throw new SemanticError(
					"An unary logical operation can be used only on " +
							"booleans.", unaryOp);
		}
		
		unaryOp.setNodeType(type); //since we know type is boolean :)
		return type;
	}

	@Override
	public Object visit(Literal literal) {
				
		Type type = null;
		
		switch (literal.getType()) {
			case INTEGER:

				//check that literal is [-2^31, 2^31-1]
				try {
					//Java signed integer is 32-bit.
					//moreover, Integer.MIN_VALUE = -2^31 and Integer.MAX_VALUE = 2^31-1
					Integer.parseInt(literal.getValue() + "");
				} catch (NumberFormatException e) {
					throw new SemanticError("Integer literal must be in the range "
							+ "between -2^31 and 2^31-1 (signed 32-bit integer).", literal);
				}
				
				type = ((PrimitiveTypeEntry)typeTable.get(
							DataTypes.INT.getDescription())).getPrimitiveType();
				break;
			case STRING:
				type = ((PrimitiveTypeEntry)typeTable.get(
						DataTypes.STRING.getDescription())).getPrimitiveType();
				break;
			case TRUE:
			case FALSE:
				type = ((PrimitiveTypeEntry)typeTable.get(
						DataTypes.BOOLEAN.getDescription())).getPrimitiveType();
				break;
			case NULL:
				type = ((PrimitiveTypeEntry)typeTable.get(
						DataTypes.NULL.getDescription())).getPrimitiveType();
				break;
		}
		
		literal.setNodeType(type);
		
		return type;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		Type type = (Type)expressionBlock.getExpression().accept(this);
		expressionBlock.setNodeType(type);
		return type;
	}

}
