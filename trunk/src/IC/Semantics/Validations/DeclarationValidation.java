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
	
	private Symbol validateDeclaration(String id, ASTNode node, Kind kind, Scope scope) {
		
		Symbol symbol = findSymbol(id, kind, scope);
		
		if (symbol == null)
			throw new SemanticError(kind.getValue() + " '" + id + "' hasn't been declared in scope " + scope.getID() + ".", node.getLine(), node.getColumn());
		
		return symbol;
	}
	
	private Symbol validateDeclaration(String id, ASTNode node, List<Kind> kinds, Scope scope) {
		
		if (kinds.size() <= 0)
			return null;
		
		if (kinds.size() == 1) {
			//better error message for one kind in the validate-only-one method:
			return validateDeclaration(id, node, kinds.get(0), scope);
		}
		
		String strKinds = "";
		Symbol symbol = null;
		
		for (Kind k : kinds) {
			strKinds += k.getValue().toLowerCase() + " or ";
			if ((symbol = findSymbol(id, k, scope)) != null)
				return symbol;
		}
		
		throw new SemanticError(
				"'" + id + "' hasn't been declared as either "
						+ strKinds.substring(0, strKinds.length() - " or ".length())
						+ " in scope " + scope.getID() + ".",
				node.getLine(), node.getColumn());
	}
	
	private Symbol findSymbol(String id, Kind kind, Scope scope) {
		
		if (scope == null)
			return null;
		
		if (scope.containsSymbol(id)) {
			if (scope.getSymbol(id).getKind() == kind)
				return scope.getSymbol(id);
		}
		
		return findSymbol(id, kind, scope.getParentScope());
		
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
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
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
		
		if (location.isExternal()) {
			external = (Symbol)location.getLocation().accept(this);
			if (external.getID().equals("this")) {
				//not really external...
				external = null;
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
						getClassScopeByName(location.getEnclosingScope(),
								((IC.Semantics.Scopes.UserType)external.getType()).getName()) :
						location.getEnclosingScope());
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
		
		if (call.isExternal()) {
			external = (Symbol)call.getLocation().accept(this);
			if (external.getID().equals("this")) {
				//not really external...
				external = null;
			} else {
				if (!(external.getType() instanceof IC.Semantics.Scopes.UserType))
					throw new SemanticError("'" + external.getID() + "' is not a class variable.",
							call.getLine(), call.getColumn());				
			}
		}

		Symbol method =
			validateDeclaration(call.getName(), call,
				Kind.VIRTUALMETHOD,
				//if external, validate method exists in external scope:
				(external != null) ?
						getClassScopeByName(
								call.getEnclosingScope(),
								((IC.Semantics.Scopes.UserType)external.getType()).getName()) :
						call.getEnclosingScope());
		
		for (Expression expr : call.getArguments()) {
			expr.accept(this);
		}
		
		return method;
	}

	@Override
	public Object visit(This thisExpression) {
		//do nothing
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
