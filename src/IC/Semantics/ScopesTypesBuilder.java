package IC.Semantics;

import java.util.ArrayList;

import IC.DataTypes;
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
import IC.Semantics.Scopes.BlockScope;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.ExtendedSymbol;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.MethodScope;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.Symbol;
import IC.Semantics.Types.MethodType;
import IC.Semantics.Types.Type;
import IC.Semantics.Types.TypeTable;

public class ScopesTypesBuilder implements Visitor {

	private String filename;
	
	/* hasLibrary indicates whether a Library table was supplied (mainly used
	 * for correct printing of Library as first class in table, and not by
	 * lexicographic order). */
	private boolean hasLibrary;
	
	private Program program;
	private TypeTable typeTable;
	
	public ScopesTypesBuilder(String filename, boolean hasLibrary) {
		this.filename = filename;
		this.hasLibrary = hasLibrary;
		this.typeTable = new TypeTable(filename);
	}
	
	private void generateDetailedSemanticError(Exception e, ASTNode node) {
		throw new SemanticError(e.getMessage(), node.getLine(), node.getColumn());
	}
	
	private void addPrimitiveTypes() {
		for (DataTypes dataType : DataTypes.values()) {
			typeTable.putPrimitive(dataType.getDescription(), dataType);
		}
	}
	
	//every valid IC program must have a main method - add it to the type table:
	private void addMainMethodSignature() {
		
		//DISCLAIMER: ugly up ahead, but is done to best match example printing :(
		
		//add type string[] of formal:
		Type type = new IC.Semantics.Types.PrimitiveType(DataTypes.STRING);
		type.setDimension(1);
		typeTable.put(type.getName() + "[]", type);
		
		//add method of void main(string[])
		PrimitiveType returnType = new PrimitiveType(0, 0, DataTypes.VOID);
		PrimitiveType formalType = new PrimitiveType(0, 0, DataTypes.STRING);
		formalType.incrementDimension();
		ArrayList<Formal> formals = new ArrayList<Formal>();
		formals.add(new Formal(formalType, ""));
		StaticMethod method = new StaticMethod(returnType, "main", formals, null);
		typeTable.put(new MethodType(method));
		
	}
	
	@Override
	public Object visit(Program program) {
		
		ProgramScope scope = new ProgramScope(filename, hasLibrary);
		program.setEnclosingScope(scope);
		
		this.program = program;
		
		addPrimitiveTypes();
		addMainMethodSignature();
		
		for (ICClass cls : program.getClasses()) {
			
			Scope childScope = (Scope)cls.accept(this);
			childScope.setParentScope(scope);
			
			try {
				scope.addToScope(new ExtendedSymbol(cls.getName(),
						new IC.Semantics.Types.UserType(cls.getName(), cls),
						Kind.CLASS,
						cls));
			} catch (Exception e) {
				generateDetailedSemanticError(e, program);
			}
			
			scope.addChildScope(childScope);
		}

		for (ICClass cls : program.getClasses()) {			
			
			if (cls.hasSuperClass()) {
				
				ICClass superClass = program.getClassByName(cls.getSuperClassName());
				
				if (superClass == null)
					generateDetailedSemanticError(
							new Exception("Class " + cls.getName() + " extends non-existing class "
											+ cls.getSuperClassName()), program);
				
				cls.getEnclosingScope().setParentScope(superClass.getEnclosingScope());
				
				//we changed parenthood - cls is no longer a child of Program scope, set it accordingly:
				superClass.getEnclosingScope().addChildScope(cls.getEnclosingScope());
				scope.removeChildScope(cls.getEnclosingScope());
			}
			
		}
		
		return new ScopesTypesWrapper(typeTable, scope);
	}

	@Override
	public Object visit(ICClass icClass) {
		
		Scope scope = new ClassScope(icClass.getName(), icClass);
		icClass.setEnclosingScope(scope);
		
		//add class to type table:
		typeTable.putClassType(icClass.getName(),
				new IC.Semantics.Types.UserType(icClass.getName(), icClass));
		
		for (Field f : icClass.getFields()) {
			f.setEnclosingScope(scope);
			f.accept(this); //field will add itself to class scope
		}
		
		for (Method m : icClass.getMethods()) {
			m.setEnclosingScope(scope);
			Scope methodScope = (Scope)m.accept(this); //m will add itself to class scope
			methodScope.setParentScope(scope);
			scope.addChildScope(methodScope);
		}
		
		return scope;
		
	}

	@Override
	public Object visit(Field field) {
		
		field.getType().setEnclosingScope(field.getEnclosingScope());
		
		try {
			
			field.getEnclosingScope().addToScope(
					new ExtendedSymbol(field.getName(),
							(Type)field.getType().accept(this), //will add to type table
							Kind.FIELD,
							field));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, field);
		}
				
		return null;//field returns nothing
	}
	
	private Scope visitMethod(Method method, boolean isStatic) {
		
		Scope scope = new MethodScope(method.getName());
		Scope classScope = method.getEnclosingScope();
		//overriding enclosing scope to be new scope, now that
		//you've extracted the class scope:
		method.setEnclosingScope(scope);
		
		method.getType().setEnclosingScope(method.getEnclosingScope());
		
		//add yourself to class scope:
		try {
			
			classScope.addToScope(new Symbol(method.getName(),
					(Type)method.getType().accept(this),
					isStatic ? Kind.STATICMETHOD : Kind.VIRTUALMETHOD,
					method));
			
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
		
		//add method to type table:
		typeTable.put(new MethodType(method));
		
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
		
		formal.getType().setEnclosingScope(formal.getEnclosingScope());
		
		try {
			
			formal.getEnclosingScope().addToScope(
					new Symbol(formal.getName(),
							(Type)formal.getType().accept(this), //will add to type table
							Kind.FORMAL,
							formal));
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, formal);
		}
		
		return null;//field returns nothing
	}

	@Override
	public Object visit(PrimitiveType type) {
		
		//map AST's primitive types to scope type:
		Type t = new IC.Semantics.Types.PrimitiveType(type.getPrimitiveType());

		if (t != null) {
			if (type.getDimension() > 0) {
				
				t.setDimension(type.getDimension());

				String brackets = "";
				for (int i = 0; i < type.getDimension(); i++) { 
					brackets += "[]";
				}
				
				typeTable.put(t.getName() + brackets, t);
				
			} else {
				//all primitives types were initially added to the
				//types table, so there's nothing to add here :)
			}
		}
		
		//in reality, will never return null (Parser sets type to be one of the above)
		return t;
	}

	@Override
	public Object visit(UserType type) {
		
		ICClass usertype = program.getClassByName(type.getName());

		if (usertype == null) {
			generateDetailedSemanticError(new Exception("User-type " + type.getName() + " is not defined."), type);
		}

		String brackets = "";
		for (int i = 0; i < type.getDimension(); i++) { 
			brackets += "[]";
		}

		Type t = new IC.Semantics.Types.UserType(type.getName() + brackets, usertype);
		if (type.getDimension() > 0) {
			t.setDimension(type.getDimension());
			typeTable.put(t.getName(), t);
		} else {
			typeTable.putClassType(t.getName(),
					new IC.Semantics.Types.UserType(t.getName(),
							program.getClassByName(type.getName())));
		}
		
		//result type should be the class name, without the brackets
		//(helpful in scope searches / lookups later on)
		Type resultType = new IC.Semantics.Types.UserType(type.getName(), usertype);
		resultType.setDimension(type.getDimension());
		return resultType;
	}

	@Override
	public Object visit(Assignment assignment) {

		//set enclosing scopes and accept:
		assignment.getVariable().setEnclosingScope(assignment.getEnclosingScope());
		assignment.getVariable().accept(this);
		
		assignment.getAssignment().setEnclosingScope(assignment.getEnclosingScope());
		assignment.getAssignment().accept(this);
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {

		//set enclosing scopes and accept:
		callStatement.getCall().setEnclosingScope(callStatement.getEnclosingScope());
		callStatement.getCall().accept(this);
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {

		//set enclosing scopes and accept:
		if (returnStatement.hasValue()) {
			returnStatement.getValue().setEnclosingScope(returnStatement.getEnclosingScope());
			returnStatement.getValue().accept(this);
		}
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(If ifStatement) {
		
		ifStatement.getCondition().setEnclosingScope(ifStatement.getEnclosingScope());
		ifStatement.getCondition().accept(this);
		
		ifStatement.getOperation().setEnclosingScope(ifStatement.getEnclosingScope());
		ifStatement.getOperation().accept(this);
		
		if (ifStatement.getElseOperation() != null) {
			ifStatement.getElseOperation().setEnclosingScope(ifStatement.getEnclosingScope());
			ifStatement.getElseOperation().accept(this);
		}
		
		return ifStatement.getEnclosingScope();
	}

	@Override
	public Object visit(While whileStatement) {
		
		whileStatement.getCondition().setEnclosingScope(whileStatement.getEnclosingScope());
		whileStatement.getCondition().accept(this);
		
		whileStatement.getOperation().setEnclosingScope(whileStatement.getEnclosingScope());
		whileStatement.getOperation().accept(this);
		
		return whileStatement.getEnclosingScope();
		
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
		Scope scope = new BlockScope(statementsBlock.getEnclosingScope());
		scope.setParentScope(statementsBlock.getEnclosingScope());
		statementsBlock.getEnclosingScope().addChildScope(scope);
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

		localVariable.getType().setEnclosingScope(
				localVariable.getEnclosingScope());
		
		try {
			
			localVariable.getEnclosingScope().addToScope(
					new Symbol(localVariable.getName(),
							(Type)localVariable.getType().accept(this), //will add to type table
							Kind.VARIABLE,
							localVariable));
			
			if (localVariable.hasInitValue()) {
				localVariable.getInitValue().setEnclosingScope(
						localVariable.getEnclosingScope());
				localVariable.getInitValue().accept(this);
			}
			
		} catch (Exception e) {
			generateDetailedSemanticError(e, localVariable);
		}
		
		return null; //nothing to return
	}

	@Override
	public Object visit(VariableLocation location) {

		//set enclosing scopes and accept:
		if (location.isExternal()) {
			location.getLocation().setEnclosingScope(location.getEnclosingScope());
			location.getLocation().accept(this);
		}
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {

		//set enclosing scopes and accept:
		location.getArray().setEnclosingScope(location.getEnclosingScope());
		location.getArray().accept(this);
		
		location.getIndex().setEnclosingScope(location.getEnclosingScope());
		location.getIndex().accept(this);
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(StaticCall call) {

		//set enclosing scopes and accept:
		for (Expression expr : call.getArguments()) {
			expr.setEnclosingScope(call.getEnclosingScope());
			expr.accept(this);
		}
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {

		//set enclosing scopes and accept:
		if (call.isExternal()) {
			call.getLocation().setEnclosingScope(call.getEnclosingScope());
			call.getLocation().accept(this);
		}
		
		for (Expression expr : call.getArguments()) {
			expr.setEnclosingScope(call.getEnclosingScope());
			expr.accept(this);
		}
		
		//nothing to return
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

		//set enclosing scopes and accept:
		newArray.getType().setEnclosingScope(newArray.getEnclosingScope());
		newArray.getType().accept(this);
		
		newArray.getSize().setEnclosingScope(newArray.getEnclosingScope());
		newArray.getSize().accept(this);
		
		//nothing to return
		return null;
	}

	@Override
	public Object visit(Length length) {

		//set enclosing scopes and accept:
		length.getArray().setEnclosingScope(length.getEnclosingScope());
		length.getArray().accept(this);
		
		//nothing to return
		return null;
	}
	
	@Override
	public Object visit(MathBinaryOp binaryOp) {

		//set enclosing scopes and accept:
		binaryOp.getFirstOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		binaryOp.getFirstOperand().accept(this);

		binaryOp.getSecondOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		binaryOp.getSecondOperand().accept(this);

		//nothing to return
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {

		//set enclosing scopes and accept:
		binaryOp.getFirstOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		binaryOp.getFirstOperand().accept(this);

		binaryOp.getSecondOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		binaryOp.getSecondOperand().accept(this);

		//nothing to return
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {

		//set enclosing scopes and accept:
		unaryOp.getOperand().setEnclosingScope(unaryOp.getEnclosingScope());
		unaryOp.getOperand().accept(this);

		//nothing to return
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {

		//set enclosing scopes and accept:
		unaryOp.getOperand().setEnclosingScope(unaryOp.getEnclosingScope());
		unaryOp.getOperand().accept(this);

		//nothing to return
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		//nothing declared - do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {

		//set enclosing scopes and accept:
		expressionBlock.getExpression().setEnclosingScope(expressionBlock.getEnclosingScope());
		expressionBlock.getExpression().accept(this);

		//nothing to return
		return null;
	}

}
