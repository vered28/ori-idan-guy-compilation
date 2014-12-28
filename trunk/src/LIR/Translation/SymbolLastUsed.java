package LIR.Translation;

import java.util.HashSet;
import java.util.Set;
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
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;

public class SymbolLastUsed implements Visitor {

	/* This class assigns each symbol in method or block a statement
	 * (and sometimes expression) where it is last used (=read).
	 * This will help us know when translating to LIR when
	 * registers are "dead" and can be reused. */
	
	private Statement currentStatement = null;
	private Stack<Expression> currentExpression = new Stack<Expression>();
	
	//look for instance at the following code:
	//while (x < 10)
	//	x = x + 1;
	//the assignment is a later statement in which x is used,
	//but in reality the condition will always be the last
	//expression / statement to be executed, therefore we
	//must mark x variable as being last used by the condition!
	private Set<Symbol> symbolsUsedInWhileCondition = new HashSet<Symbol>();
	//while loops may be nested, so we'd also like to keep a
	//set just for the "current" while loop condition so when
	//we exit a nested loop, we know that variables used in
	//the outter loop condition are still last used by the
	//condition:
	private Set<Symbol> symbolsUsedInCurrentWhileCondition = null;
	private boolean whileCondition = false;
	private Stack<Boolean> whileLoop = new Stack<Boolean>();
	
	//flag is false for instance for assignment (x = something()),
	//x isn't read so don't consider it as being used. If nowhere
	//else in the scope where x was declared x is read (i.e. y = x)
	//then it is simply an unused variable. We shouldn't produce
	//LIR code for it.
	private boolean considerAsUsed = true;
		
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
			currentStatement = stmt;
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		for (Statement stmt : method.getStatements()) {
			currentStatement = stmt;
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
		
		//don't mark left-hand part as "used" (it is only
		//written to, not read from. should it be read from later,
		//that it would be considered used).
		considerAsUsed = false;
		assignment.getAssignment().accept(this);
		considerAsUsed = true;
		
		assignment.getVariable().accept(this);
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
		
		currentExpression.push(ifStatement.getCondition());
		ifStatement.getCondition().accept(this);
		currentExpression.pop();
		
		currentStatement = ifStatement.getOperation();
		ifStatement.getOperation().accept(this);
		
		if (ifStatement.hasElse()) {
			currentStatement = ifStatement.getElseOperation();
			ifStatement.getElseOperation().accept(this);
		}
		
		currentStatement = ifStatement;
		
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		
		currentExpression.push(whileStatement.getCondition());
		
		//set flag to notify expressions that they should add symbols
		//to the symbolsUsedInCurrentWhileCondition set:
		whileCondition = true;
		symbolsUsedInCurrentWhileCondition = new HashSet<Symbol>();
		
		whileStatement.getCondition().accept(this);
		
		//add all symbols from this condition to all symbols from
		//other while loops that are still being executed
		// (i.e. while(condition1) { while (condition2) ... and so on } } 
		symbolsUsedInWhileCondition.addAll(
				symbolsUsedInCurrentWhileCondition);
		
		//save a local copy of this while loop's condition symbols:
		//(symbolsUsedInCurrentWhileCondition will be overriden by any
		// nested loop):
		Set<Symbol> thisWhileConditionSymbols = new HashSet<Symbol>(symbolsUsedInCurrentWhileCondition);
		
		whileCondition = false;
		currentExpression.pop();
		
		//while inside loop operation, don't mark symbols existing in
		//symbolsUsedInWhileCondition as used:
		whileLoop.push(true);
		
		currentStatement = whileStatement.getOperation();
		whileStatement.getOperation().accept(this);
		currentStatement = whileStatement;
		
		whileLoop.pop();
		
		//remove current loop condition symbols from overall
		//condition symbols:
		symbolsUsedInWhileCondition.removeAll(
				thisWhileConditionSymbols);
		
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
			currentStatement = stmt;
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
		
		if (location.isExternal()) {
			location.getLocation().accept(this);
		} else {
		
			if (!considerAsUsed)
				return null;
			
			Symbol sym =
					ScopesTraversal.findSymbol(location.getName(), Kind.VARIABLE,
							location.getEnclosingScope(),
							location.getLine(),
							true /*variables only declared in method scope*/);
			if (sym != null) {
				
				//only mark this statement / exprsssion as last using the local
				//variable if not inside a while loop or if inside a while loop
				//but symbol is not used in while condition:
				if (whileLoop.isEmpty() ||
						(!whileLoop.isEmpty() && !symbolsUsedInWhileCondition.contains(sym))) {
					
					sym.setLastStatementUsed(currentStatement);
					if (!currentExpression.isEmpty()) {
						sym.setLastExpressionUsed(currentExpression.peek());
					} else {
						//currentStatement is more advanced than expression,
						//last expression used is residue from old usage,
						//remove it:
						sym.setLastExpressionUsed(null);
					}
					
				}
				
				//if inside while condition, add symbol to current condition symbols:
				if (whileCondition) {
					symbolsUsedInCurrentWhileCondition.add(sym);
				}
			}
			
		}
		
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		Object array = location.getArray().accept(this);
		location.getIndex().accept(this);
		return array;
	}

	@Override
	public Object visit(StaticCall call) {
		for (Expression expr : call.getArguments()) {
			currentExpression.push(expr);
			expr.accept(this);
			currentExpression.pop();
		}
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		
		if (call.isExternal())
			call.getLocation().accept(this);
		
		for (Expression expr : call.getArguments()) {
			currentExpression.push(expr);
			expr.accept(this);
			currentExpression.pop();
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
		//do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		expressionBlock.getExpression().accept(this);
		return null;
	}

}
