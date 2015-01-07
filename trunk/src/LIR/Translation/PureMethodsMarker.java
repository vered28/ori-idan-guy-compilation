package LIR.Translation;

import java.util.Arrays;
import java.util.List;

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
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;

public class PureMethodsMarker implements Visitor {

	private Method currentMethod;
	private boolean pureness;
	
	private boolean assignmentFlag = false;
	
	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine) {
		
		Symbol symbol;
		for (Kind k : kinds) {
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine)) != null)
				return symbol;
		}

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
		for (Method method : icClass.getMethods()) {
			pureness = true; //assume true, contradict when can
			currentMethod = method;
			method.accept(this);
			currentMethod.setPure(pureness);
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
		pureness = false;
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
		//do nothing
		return null;
	}

	@Override
	public Object visit(Assignment assignment) {
		
		assignment.getAssignment().accept(this);
		
		assignmentFlag = true;
		assignment.getVariable().accept(this);
		assignmentFlag = false;

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
		//can read to be pure, cannot write!
		if (assignmentFlag) {
			
			if (location.isExternal()) {
				pureness = false;
				return null;
			}
			
			//not external... see if formal or field.
			//field --> not pure!
			//formal --> pure if primitive non-array type!
			
			Symbol symbol = findSymbol(location.getName(),
					Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
					location.getEnclosingScope(),
					location.getLine());
			
			switch (symbol.getKind()) {
				case VARIABLE:
					return null;
				case FIELD:
					pureness = false;
				case FORMAL:
					IC.Semantics.Types.Type type = symbol.getType();
					if (type.getDimension() > 0 ||
							!(type instanceof IC.Semantics.Types.PrimitiveType)) {
						pureness = false;
					}
				default:
					//do nothing -- won't get here!
			}
		}
		
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
		
		//only pure if method is pure:
		
		Symbol symbol = ScopesTraversal.findSymbol(call.getName(),
				Kind.STATICMETHOD,
				ScopesTraversal.getClassScopeByName(
						call.getEnclosingScope(),
						call.getClassName()));
		
		if (symbol != null) {
			//remember, we assume all methods are impure, unless
			//we've managed to proved they are. yet unchecked
			//methods, even if pure, are considered impure.
			if (!((Method)symbol.getNode()).isPure())
				pureness = false;
		}

		return null;
	}

	@Override
	public Object visit(VirtualCall call) {

		//the catch for virtual call -- it may also be a static
		//call to a static method in the same class scope.
		
		boolean virtualMethod = true;
		
		if (call.isExternal()) {
			//be a harsh bastard - assume impureness
			pureness = false;
			return null;
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
		
		if (methodSymbol != null) {
			if (!((Method)methodSymbol.getNode()).isPure()) {
				pureness = false;
			}
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
		// do nothing
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
		expressionBlock.getExpression().accept(this);
		return null;
	}

}
