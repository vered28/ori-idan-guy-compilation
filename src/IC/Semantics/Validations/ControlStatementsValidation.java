package IC.Semantics.Validations;

import java.util.Stack;

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

public class ControlStatementsValidation implements Visitor {

	/* This class checks that control statements appear
	 * only in the right form / place.
	 * 
	 * # break; and continue; commands must only appear inside loops
	 * 			(currently only loop in IC language is while loop).
	 * 
	 * # this can only be used in instance method (non-static methods)
	 */
	
	//push flag whenever checking while statement. As long as stack is not
	//empty, we're inside a while loop (using stack because of nested loops possibility)
	private Stack<Boolean> whileStack = new Stack<Boolean>();
	
	//raise flag before visiting static method tree, and lower it when done
	private boolean staticMethod = false;
	
	@Override
	public Object visit(Program program) {
		for (ICClass cls : program.getClasses()) {
			cls.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {
		//no need to check fields, only methods can have statements:
		for (Method m : icClass.getMethods()) {
			m.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(Field field) {
		// do nothing (unreachable in this context)
		return null;
	}

	@Override
	public Object visit(VirtualMethod method) {
		//only go over statements, not formals:
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {

		//only go over statements, not formals:
		staticMethod = true;
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		staticMethod = false;
		
		return null;
	}

	@Override
	public Object visit(LibraryMethod method) {
		// do nothing (has no implementation and therefore no statements)
		return null;
	}

	@Override
	public Object visit(Formal formal) {
		//do nothing (formals are not statements and therefore not checked)
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
		
		//raise flag only when checking statement(s) inside loop:
		whileStack.push(true);
		whileStatement.getOperation().accept(this);
		whileStack.pop();
		
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		if (whileStack.size() == 0) {
			throw new SemanticError("break must only appear inside loop",
					breakStatement.getLine(),
					breakStatement.getColumn());
		}
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		if (whileStack.size() == 0) {
			throw new SemanticError("continue must only appear inside loop",
					continueStatement.getLine(),
					continueStatement.getColumn());
		}
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
		if (staticMethod) {
			throw new SemanticError("this keyword can only be used within instance methods.",
					thisExpression.getLine(),
					thisExpression.getColumn());
		}
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		//do nothing
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
		// do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		expressionBlock.getExpression().accept(this);
		return null;
	}

}
