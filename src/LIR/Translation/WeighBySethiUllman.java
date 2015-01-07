package LIR.Translation;

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
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;

public class WeighBySethiUllman implements Visitor {

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
		location.setWeight(1);
		return 1;
	}

	@Override
	public Object visit(ArrayLocation location) {
		location.getArray().accept(this);
		location.getIndex().accept(this);
		location.setWeight(1);
		return 1;
	}

	@Override
	public Object visit(StaticCall call) {
		
		Symbol symbol = ScopesTraversal.findSymbol(call.getName(),
				Kind.STATICMETHOD,
				ScopesTraversal.getClassScopeByName(
						call.getEnclosingScope(),
						call.getClassName()));
		
		if (((Method)symbol.getNode()).isPure()) {
			call.setWeight(1);
			return 1;
		}

		call.setWeight(-1);
		return -1;
	}

	@Override
	public Object visit(VirtualCall call) {

		//the catch for virtual call -- it may also be a static
		//call to a static method in the same class scope.
		
		boolean virtualMethod = true;
		
		if (call.isExternal()) {
			//we don't know how to tell if external method
			//calls are pure...
			return -1;
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
			if (((Method)methodSymbol.getNode()).isPure()) {
				call.setWeight(1);
				return 1;
			}
		}
		
		call.setWeight(-1);
		return -1;
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
		length.setWeight(1);
		return 1; //no weight
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		
		int op1 = (Integer)binaryOp.getFirstOperand().accept(this);
		int op2 = (Integer)binaryOp.getSecondOperand().accept(this);
		
		if (op1 == -1 || op2 == -1) {
			//cannot avoid side-effects
			binaryOp.setWeight(-1);
			return -1;
		}
		
		binaryOp.setWeight(op1+op2);
		return op1+op2;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {

		int op1 = (Integer)binaryOp.getFirstOperand().accept(this);
		int op2 = (Integer)binaryOp.getSecondOperand().accept(this);
		
		if (binaryOp.getOperator().isLogicalOperation()) {
			//in && or ||, short-circuit, order matters!
			binaryOp.setWeight(-1);
			return -1;
		}
		
		if (op1 == -1 || op2 == -1) {
			//cannot avoid side-effects
			binaryOp.setWeight(-1);
			return -1;
		}
		
		binaryOp.setWeight(op1+op2);
		return op1+op2;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		int weight = (Integer)unaryOp.getOperand().accept(this);
		unaryOp.setWeight(weight);
		return weight;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		int weight = (Integer)unaryOp.getOperand().accept(this);
		unaryOp.setWeight(weight);
		return weight;
	}

	@Override
	public Object visit(Literal literal) {
		literal.setWeight(1);
		return 1;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		int weight = (Integer)expressionBlock.getExpression().accept(this);
		expressionBlock.setWeight(weight);
		return weight;
	}

}
