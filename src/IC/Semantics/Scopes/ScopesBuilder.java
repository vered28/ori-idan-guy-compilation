package IC.Semantics.Scopes;

import IC.DataTypes;
import IC.AST.ASTNode;
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

public class ScopesBuilder implements Visitor {

	private void generateDetailedSemanticError(Exception e, ASTNode node) {
		//TODO: set column in ASTNode for printing purposes
		throw new SemanticError(e.getMessage(), node.getLine(), 1);
	}
	
	@Override
	public Object visit(Program program) {
		
		Scope scope = new ProgramScope();
		program.setEnclosingScope(scope);
		
		for (ICClass cls : program.getClasses()) {
			
			Scope childScope = (Scope)cls.accept(this);
			childScope.setParentScope(scope);
			
			try {
				scope.addToScope(new Symbol(cls.getName(), Type.USERTYPE, Kind.CLASS));
			} catch (Exception e) {
				generateDetailedSemanticError(e, program);
			}
		}

		return scope;
	}

	@Override
	public Object visit(ICClass icClass) {
		
		Scope scope = new ClassScope(icClass.getName());
		icClass.setEnclosingScope(scope);
		
		for (Field f : icClass.getFields()) {
			f.setEnclosingScope(scope);
			f.accept(this); //field will add itself to class scope
		}
		
		for (Method m : icClass.getMethods()) {
			m.setEnclosingScope(scope);
			Scope methodScope = (Scope)m.accept(this); //m will add itself to class scope
			methodScope.setParentScope(scope);			
		}
		
		return scope;
		
	}

	@Override
	public Object visit(Field field) {
		
		try {
			
			field.getEnclosingScope().addToScope(
					new Symbol(field.getName(),
							(Type)field.getType().accept(this),
							Kind.VAR));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, field);
		}
		
		return null;//field returns nothing
	}
	
	private Scope visitMethod(Method method, boolean isStatic) {
		
		Scope scope = new Scope(method.getName());
		Scope classScope = method.getEnclosingScope();
		//overriding enclosing scope to be new scope, now that
		//you've extracted the class scope:
		method.setEnclosingScope(scope);
		
		//add yourself to class scope:
		try {
			
			classScope.addToScope(new Symbol(method.getName(),
					(Type)method.getType().accept(this),
					Kind.METHOD,
					isStatic));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, method);
		}
		
		//add formals to the method scope:
		for (Formal f : method.getFormals()) {
			f.setEnclosingScope(scope);
			f.accept(this); //formal will add itself to the scope
		}
		
		//visit statements and add to scope if necessary:
		for (Statement stmt : method.getStatements()) {
			stmt.setEnclosingScope(scope);
			stmt.accept(this);
		}
		
		return scope;

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
		//same as static mehtod. Note statements will be an empty array,
		//so in reality for (Statements) loop will not occur for library methods
		return visitMethod(method, true);
	}

	@Override
	public Object visit(Formal formal) {
		
		try {
			
			formal.getEnclosingScope().addToScope(
					new Symbol(formal.getName(),
							(Type)formal.getType().accept(this),
							Kind.VAR));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, formal);
		}
		
		return null;//field returns nothing
	}

	@Override
	public Object visit(PrimitiveType type) {
		
		//map AST's primitive types to scope type:
		String astType = type.getName();
		
		if (astType.equals(DataTypes.INT.getDescription())) {
			return Type.INT;
		}
		
		if (astType.equals(DataTypes.BOOLEAN.getDescription())) {
			return Type.BOOLEAN;
		}

		if (astType.equals(DataTypes.VOID.getDescription())) {
			return Type.VOID;
		}

		if (astType.equals(DataTypes.STRING.getDescription())) {
			return Type.STRING;
		}

		return null; //in reality, will never return null (Parser sets type to be one of the above)
	}

	@Override
	public Object visit(UserType type) {
		return Type.USERTYPE;
	}

	@Override
	public Object visit(Assignment assignment) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(If ifStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) {

		//define new scope under current enclosing scope:
		Scope scope = new BlockScope();
		scope.setParentScope(statementsBlock.getEnclosingScope());
		//override enclosing scope to be new scope:
		statementsBlock.setEnclosingScope(scope);
		
		//visit all statements in scope:
		for (Statement stmt : statementsBlock.getStatements()) {
			stmt.setEnclosingScope(scope);
			stmt.accept(this);
		}

		return scope;
		
	}

	@Override
	public Object visit(LocalVariable localVariable) {

		try {
			
			localVariable.getEnclosingScope().addToScope(
					new Symbol(localVariable.getName(),
							(Type)localVariable.getType().accept(this),
							Kind.VAR));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, localVariable);
		}
		
		return null; //nothing to return
	}

	@Override
	public Object visit(VariableLocation location) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(StaticCall call) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(Length length) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		//nothing declared - do nothing
		return null;
	}

}