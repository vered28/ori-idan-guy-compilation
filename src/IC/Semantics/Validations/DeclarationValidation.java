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
import IC.Semantics.SemanticError;
import IC.Semantics.StaticVirtualAmbiguityException;
import IC.Semantics.Scopes.BlockScope;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.MethodScope;
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
	private boolean staticMethod = true;
	
	private Symbol validateDeclaration(String id, ASTNode node, Kind kind, Scope scope) {
		return validateDeclaration(id, node, kind, scope, false);
	}
	
	private Symbol validateDeclaration(String id, ASTNode node, Kind kind, Scope scope, boolean onlyCheckInMethodScope) {
		
		Symbol symbol = findSymbol(id, kind, scope, onlyCheckInMethodScope);
		
		if (symbol == null)
			throw new SemanticError(kind.getValue() + " '" + id + "' hasn't been declared in scope " + scope.getID() + ".", node.getLine(), node.getColumn());
		
		return symbol;
	}
	
	private Symbol validateDeclaration(String id, ASTNode node, List<Kind> kinds, Scope scope, boolean onlyCheckInMethodScope) {
		
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
			if ((symbol = findSymbol(id, k, scope, onlyCheckInMethodScope)) != null)
				return symbol;
		}
		
		throw new SemanticError(
				"'" + id + "' hasn't been declared as either "
						+ strKinds.substring(0, strKinds.length() - " or ".length())
						+ " in scope " + scope.getID() + ".",
				node.getLine(), node.getColumn());
	}
	
	private Symbol findSymbol(String id, Kind kind, Scope scope) {
		return findSymbol(id, kind, scope, false);
	}
	
	private Symbol findSymbol(String id, Kind kind, Scope scope, boolean onlyCheckInMethodScope) {
		
		if (scope == null || (onlyCheckInMethodScope && !(scope instanceof MethodScope)))
			return null;
		
		try {
			if (scope.containsSymbol(id)) {
				if (scope.getSymbol(id).getKind() == kind)
					return scope.getSymbol(id);
			}
		} catch (StaticVirtualAmbiguityException e) {
			if (kind.equals(Kind.STATICMETHOD)) {
				Symbol sym = ((ClassScope)scope).getStaticSymbol(id);
				if (sym != null)
					return sym;
			} else if (kind.equals(Kind.VIRTUALMETHOD)) {
				Symbol sym = ((ClassScope)scope).getSymbol(id, true);
				if (sym != null)
					return sym;				
			}
		}
		
		return findSymbol(id, kind, scope.getParentScope(), onlyCheckInMethodScope);
		
	}
	
	private Scope getClassScopeByName(Scope currentScope, String className) {
		
		//run current until root (ProgramScope):
		while (currentScope.getParentScope() != null) {
			currentScope = currentScope.getParentScope();
		}
		
		//run over all symbols (all program classes) and return
		//scope of class with given ID className:
		for (Symbol symbol : currentScope.getSymbols()) {
			if (symbol.getKind() == Kind.CLASS) { //all should be classes, but make sure anyway
				if (symbol.getID().equals(className))
					return symbol.getNode().getEnclosingScope();
			}
		}
		
		//no class found by that name (Symbol ID), return null:
		return null;
	}
	
	private Scope getClassScopeOfCurrentScope(Scope currentScope) {
		
		if (currentScope == null)
			return null;
		
		if (currentScope instanceof MethodScope
				&& !(currentScope instanceof BlockScope))
			return currentScope.getParentScope(); //parent of method is always class
		
		return getClassScopeOfCurrentScope(currentScope.getParentScope());
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
		
		//no need to visit fields (cannot be initialized and therefore
		//cannot call any other field / variable or method).
		
		for (Method method : icClass.getMethods()) {
			method.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(Field field) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VirtualMethod method) {
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		
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
		if (getClassScopeByName(
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
				if (!(external.getType() instanceof IC.Semantics.Scopes.UserType))
					throw new SemanticError("'" + external.getID() + "' is not a class variable.",
							location.getLine(), location.getColumn());
			}
		}
		
		//if valid, return the symbol of the location:
		return validateDeclaration(location.getName(), location,
				Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
				//if external, search in external symbol scope, and not in current scope:
				(external != null) ?
						((thisKeyword) ? getClassScopeOfCurrentScope(location.getEnclosingScope())
										: getClassScopeByName(location.getEnclosingScope(),
																((IC.Semantics.Scopes.UserType)external.getType()).getName()))
						: location.getEnclosingScope(),
				(external == null && this.staticMethod) /* if in static method,
															only search in method scope (class scope inaccessible) */);
	}

	@Override
	public Object visit(ArrayLocation location) {
		Symbol symbol = (Symbol)location.getArray().accept(this);
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
				getClassScopeByName(call.getEnclosingScope(),
										call.getClassName())
				);
		
		//check call is legal (supplies as many parameters as needed):
		int formalsCount = ((Method)method.getNode()).getFormals().size();
		int argumentsCount = call.getArguments().size();
		if (formalsCount != argumentsCount) {
			throw new SemanticError(
					"method call to '" + method.getID() + "' expects " + formalsCount
					+ ", supplied " + argumentsCount + ".",
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
		//as a virtual method call. when set to false, 
		boolean virtualMethod = true;
		
		if (call.isExternal()) {
			external = (Symbol)call.getLocation().accept(this);
			if (external.getID().equals("this")) {
				thisKeyword = true;
			} else {
				if (!(external.getType() instanceof IC.Semantics.Scopes.UserType))
					throw new SemanticError("'" + external.getID() + "' is not a class variable.",
							call.getLine(), call.getColumn());				
			}
		} else {
			
			//this could be a static call reference if there are both a static
			//and a virtual methods with call.getName() declared in this scope.
			
			ClassScope currentScope = (ClassScope)call.getEnclosingScope();
			Symbol staticSymbol = findSymbol(call.getName(), Kind.STATICMETHOD, currentScope);
			
			if (staticSymbol != null) {
				
				Symbol virtualSymbol = findSymbol(call.getName(), Kind.VIRTUALMETHOD, currentScope);
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
							((thisKeyword) ? getClassScopeOfCurrentScope(call.getEnclosingScope())
										  : getClassScopeByName(
												call.getEnclosingScope(),
												((IC.Semantics.Scopes.UserType)external.getType()).getName()))
							: call.getEnclosingScope());
		
		//check call is legal (supplies as many parameters as needed):
		int formalsCount = ((Method)method.getNode()).getFormals().size();
		int argumentsCount = call.getArguments().size();
		if (formalsCount != argumentsCount) {
			throw new SemanticError(
					"method call to '" + method.getID() + "' expects " + formalsCount
					+ ", supplied " + argumentsCount + ".",
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