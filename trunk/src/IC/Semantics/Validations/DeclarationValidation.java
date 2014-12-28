package IC.Semantics.Validations;

import java.util.Arrays;
import java.util.List;

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
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.Symbol;

public class DeclarationValidation implements Visitor {

	/* 
	 * This class traverses the AST and checks that each
	 * reference to a variable or method (a.k.a call) exists
	 * meaning if (x == y && checkThis(z)): validates that
	 * 	1. variables x, y, z have been declared
	 *  2. checkThis() has been declared and received one argument
	 * 
	 * Note: type-checking is done at a different phase
	 * 
	 */
	
	//flag is raised when checking validity of statements and their children inside
	//static methods. Inside static methods, cannot reference class fields, only
	//local variables and other static methods inside the class.
	private boolean staticMethod = false;
		
	private Symbol validateDeclaration(String id, ASTNode node, Kind kind, Scope scope) {
		return validateDeclaration(id, node, kind, scope, false);
	}
	
	private Symbol validateDeclaration(String id, ASTNode node, Kind kind, Scope scope, boolean onlyCheckInMethodScope) {
		
		Symbol symbol = ScopesTraversal.findSymbol(
				id, kind, scope, onlyCheckInMethodScope);
		
		if (symbol == null)
			throw new SemanticError(kind.getValue() + " '" + id + "' hasn't been declared in scope " + scope.getID() + ".", node.getLine(), node.getColumn());
		
		return symbol;
	}
	
	private Symbol validateDeclaration(String id, ASTNode node, List<Kind> kinds, Scope scope, int aboveLine, boolean onlyCheckInMethodScope) {
		
		if (kinds.size() <= 0)
			return null;
		
		if (kinds.size() == 1) {
			//better error message for one kind in the validate-only-one method:
			return validateDeclaration(id, node, kinds.get(0), scope, onlyCheckInMethodScope);
		}
		
		String strKinds = "";
		Symbol symbol = null;
		
		for (Kind k : kinds) {
			strKinds += k.getValue().toLowerCase() + " or ";
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine, onlyCheckInMethodScope)) != null)
				return symbol;
		}
		
		throw new SemanticError(
				"'" + id + "' hasn't been declared as either "
						+ strKinds.substring(0, strKinds.length() - " or ".length())
						+ " in scope " + scope.getID() + ".",
				node.getLine(), node.getColumn());
	}
		
	@Override
	public Object visit(Program program) {
		for (ICClass cls : program.getClasses()) {
			cls.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {
		
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
		
		//fields cannot be initialized so not a lot to check here.
		//the only potential problem is if field is defined twice,
		//once in base class and once in drive class.
		//----------------------------------------------------------
		//regular insertion to scope already checks same field isn't
		//added twice to the same scope.
		
		ClassScope currentScope = (ClassScope)field.getEnclosingScope();
		if (currentScope.getParentScope() instanceof ClassScope) {
			
			//class extends another class - potential for field overloading:
			
			Symbol field2 = ScopesTraversal.findSymbol(
					field.getName(), Kind.FIELD, currentScope.getParentScope());
			
			if (field2 != null) {
				throw new SemanticError("field '" + field.getName() + "' cannot be declared "
						+ "in class " + currentScope.getID() + " because it overloads "
						+ "field in super class.",
						field.getLine(), field.getColumn());
			}
		}

		//nothing to return
		return null;
	}

	private void checkLegalOverride(Method method, boolean isStatic) {
		
		ClassScope scope = (ClassScope)method.getEnclosingScope().getParentScope();
		if (scope.getParentScope() instanceof ClassScope) {
			
			//method is defined within a derive class, it may be an override.
			//an override is only legal if it does not change the original
			//method's signature. We'll only check number of formals here.
			//in TypeValidation we'll check for correct types.
			
			Symbol originalMethod = ScopesTraversal.findSymbol(method.getName(),
					isStatic ? Kind.STATICMETHOD : Kind.VIRTUALMETHOD,
					scope.getParentScope());
			
			if (originalMethod != null) {
				
				//we've found a method with the same name in a super class
				//(searched from parent higher). check number of formals
				//match:
				
				if (((Method)originalMethod.getNode()).getFormals().size()
						!= method.getFormals().size()) {
					throw new SemanticError("method '" + method.getName() + "' override in class " + scope.getID() 
							+ " is illegal due to a different number of formals.",
							method.getLine(), method.getColumn());
				}
				
			}
		}
		
	}
	
	@Override
	public Object visit(VirtualMethod method) {
		checkLegalOverride(method, false);
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		
		checkLegalOverride(method, true);
		
		staticMethod = true;
		
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		
		staticMethod = false;
		
		return null;
		
	}

	@Override
	public Object visit(LibraryMethod method) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Formal formal) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(PrimitiveType type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(UserType type) {

		//check that type has been declared (there exists a class type.getName()):
		if (ScopesTraversal.getClassScopeByName(
				type.getEnclosingScope(), type.getName()) == null) {
			throw new SemanticError("'" + type.getName()
					+ "' class has not been declared.",
					type.getLine(),
					type.getColumn());
				
		}
		
		return null;
	}

	@Override
	public Object visit(Assignment assignment) {
		assignment.getVariable().accept(this);		
		assignment.getAssignment().accept(this);
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
		
		//nothing to return:
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
		//do nothing
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		//do nothing
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
				
		Symbol external = null;
		boolean thisKeyword = false;
		
		if (location.isExternal()) {
			external = (Symbol)location.getLocation().accept(this);
			if (external.getID().equals("this")) {
				thisKeyword = true;
			} else {
				if (!(external.getType() instanceof IC.Semantics.Types.UserType))
					throw new SemanticError("'" + external.getID() + "' is not a class variable.",
							location.getLine(), location.getColumn());
			}
		}
		
		//if valid, return the symbol of the location:
		return validateDeclaration(location.getName(), location,
				Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
				//if external, search in external symbol scope, and not in current scope:
				(external != null) ?
						((thisKeyword) ? ScopesTraversal.getClassScopeOfCurrentScope(location.getEnclosingScope())
										: ScopesTraversal.getClassScopeByName(location.getEnclosingScope(),
																((IC.Semantics.Types.UserType)external.getType()).getName()))
						: location.getEnclosingScope(),
				location.getLine(),
				(external == null && this.staticMethod) /* if in static method,
															only search in method scope (class scope inaccessible) */);
	}

	@Override
	public Object visit(ArrayLocation location) {
		
		Symbol symbol = (Symbol)location.getArray().accept(this);
		
		//if symbol is null, then this is probably an array initialization.
		//check legality at type checking phase, not here.
		
		if (symbol != null) {
			//check that location call is with accordance to array dimensions
			//(so that is array is for example 2-dimensional, we don't call [x][y][z]).
			if (location.getArray() instanceof ArrayLocation) {

				int countDimensions = 1;
				ArrayLocation array = (ArrayLocation)location.getArray();
				
				while (array != null) {
				
					if (!(array.getArray() instanceof ArrayLocation))
						break;
					
					array = (ArrayLocation)array.getArray();
					countDimensions++;
				}
				
				//add one to countDimensions because the last dimension location
				//is not counted (is probably a VariableLocation).
				if (countDimensions + 1 > symbol.getType().getDimension())
					throw new SemanticError(symbol.getID() + " only has "
												+ symbol.getType().getDimension() + " dimensions.",
											location.getLine(), location.getColumn());
			}
		}

		
		location.getIndex().accept(this);
		return symbol;
		
	}

	@Override
	public Object visit(StaticCall call) {

		validateDeclaration(call.getClassName(), call,
				Kind.CLASS, call.getEnclosingScope());

		Symbol method =
			validateDeclaration(call.getName(), call,
				Kind.STATICMETHOD,
				ScopesTraversal.getClassScopeByName(call.getEnclosingScope(),
										call.getClassName())
				);
		
		//check call is legal (supplies as many parameters as needed):
		int formalsCount = ((Method)method.getNode()).getFormals().size();
		int argumentsCount = call.getArguments().size();
		if (formalsCount != argumentsCount) {
			throw new SemanticError(
					"method call to '" + method.getID() + "' expects " + formalsCount
					+ " parameters, supplied " + argumentsCount + ".",
					call.getLine(),
					call.getColumn());
		}

		//on the surface, this static call is legal; check arguments are legal too:
		for (Expression expr : call.getArguments()) {
			expr.accept(this);
		}
		
		return method;
	}

	@Override
	public Object visit(VirtualCall call) {
				
		Symbol external = null;
		boolean thisKeyword = false;
		
		//virtualMethod flag marks that this call should be treated
		//as a virtual method call. when set to false, it is treated
		//as a static call.
		boolean virtualMethod = true;
		
		if (call.isExternal()) {
			external = (Symbol)call.getLocation().accept(this);
			if (external.getID().equals("this")) {
				thisKeyword = true;
			} else {
				if (!(external.getType() instanceof IC.Semantics.Types.UserType))
					throw new SemanticError("'" + external.getID() + "' is not a class variable.",
							call.getLine(), call.getColumn());				
			}
		} else {
			
			//this could be a static call reference if there are both a static
			//and a virtual method with call.getName() declared in this scope.
			
			ClassScope currentScope = (ClassScope)call.getEnclosingScope();
			Symbol staticSymbol = ScopesTraversal.findSymbol(call.getName(), Kind.STATICMETHOD, currentScope);
			
			if (staticSymbol != null) {
				
				Symbol virtualSymbol = ScopesTraversal.findSymbol(call.getName(), Kind.VIRTUALMETHOD, currentScope);
				//if number of arguments is different, but one of them matches
				//this call's arguments count, choose the one that matches.
				//otherwise, throw an excpetion:
				
				if (virtualSymbol == null) {
					
					//this is a static call:
					virtualMethod = false;
					
				} else {
				
					String errorMsg = "Ambigious call to method " + call.getName() +
							". If you're trying to access a static method, add the class name infront of it (i.e. C.method), "
							+ "and if you're trying to access a virtual method, use the this keyword (i.e. this.method).";
					
					int staticFormalsCount =  ((Method)staticSymbol.getNode())
							.getFormals().size();
					int virtualFormalsCount = ((Method)virtualSymbol.getNode())
							.getFormals().size();
					
					if (staticFormalsCount != virtualFormalsCount) {
						int argumentsSize = call.getArguments().size();
						if (staticFormalsCount == argumentsSize) {
							virtualMethod = false;
						} else if (virtualFormalsCount == argumentsSize) {
							virtualMethod = true; //set to the same value, don't change
						} else {
							throw new SemanticError(errorMsg, call.getLine(), call.getColumn());
						}
					} else {
						throw new SemanticError(errorMsg, call.getLine(), call.getColumn());
					}
				}
				
			}
		}

		if (external == null && virtualMethod && this.staticMethod){
			//cannot call virtual method of this class from within a static
			//method implementation:
			throw new SemanticError("cannot call virtual methods from static scope",
					call.getLine(), call.getColumn());
		}
		
		Symbol method =
			validateDeclaration(call.getName(), call,
				virtualMethod ? Kind.VIRTUALMETHOD : Kind.STATICMETHOD,
				//if external, validate method exists in external scope:
				(external != null) ?
							((thisKeyword) ? ScopesTraversal.getClassScopeOfCurrentScope(call.getEnclosingScope())
										  : ScopesTraversal.getClassScopeByName(
												call.getEnclosingScope(),
												((IC.Semantics.Types.UserType)external.getType()).getName()))
							: call.getEnclosingScope());
		
		//check call is legal (supplies as many parameters as needed):
		int formalsCount = ((Method)method.getNode()).getFormals().size();
		int argumentsCount = call.getArguments().size();
		if (formalsCount != argumentsCount) {
			throw new SemanticError(
					"method call to '" + method.getID() + "' expects " + formalsCount
					+ " parameters, supplied " + argumentsCount + ".",
					call.getLine(),
					call.getColumn());
		}
		
		for (Expression expr : call.getArguments()) {
			expr.accept(this);
		}
		
		return method;
	}

	@Override
	public Object visit(This thisExpression) {
		if (staticMethod) {
			//this is a big no-no: cannot access "this" since we are static, not an instance
			throw new SemanticError("Static class cannot use the this keyword",
					thisExpression.getLine(), thisExpression.getColumn());
		}
		return new Symbol("this", null, null, null);
	}

	@Override
	public Object visit(NewClass newClass) {
				
		validateDeclaration(newClass.getName(),
				newClass, Kind.CLASS, newClass.getEnclosingScope());
		
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		newArray.getType().accept(this);
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
		//do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		return expressionBlock.getExpression().accept(this);
	}

}
