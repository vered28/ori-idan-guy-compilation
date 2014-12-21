package IC.Semantics.Validations;

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
import IC.Semantics.Exceptions.DeadCodeException;
import IC.Semantics.Exceptions.SemanticError;

public class NonVoidAlwaysReturnsValidation implements Visitor {

	/* 
	 * This class traverses the AST and makes sure each non void
	 * method returns a value on every path. We've already checked
	 * that each return statement is in the correct return type
	 * of the method (in TypeValidation).
	 * 
	 * When does a method may have more than one path? Whenever
	 * it branches. Branches in Ice Coffee are If conditions
	 * (including possible Else part) and While loops.
	 * 
	 * We cannot guarantee while will be executed, so the only
	 * way to ensure return on all code paths, when method
	 * scope has no return statement, is by making sure both 
	 * if and else have a return statement.
	 *	 
	 */
	
	//flag is true when we've exhausted a superficial search for a
	//return statement and want to go deeper (in the superficial
	//search we don't go inside if/else conditions).
	boolean visitStatementBlockChildren = false;
	
	private boolean returnStatementExists(List<Statement> statements) {
		
		boolean returnFound = false;
		
		//go over all statements and check two things:
		// 1. if return statement exists.
		// 2. if statements exist after return statement (dead code)
		
		for (Statement stmt : statements) {
			
			if (returnFound)
				throw new DeadCodeException("statement appearing after return " +
						"statement is unreachable.", stmt);
			
			if (stmt instanceof Return)
				returnFound = true;
			
			//an independent statements block (not inside if / while),
			//can have a valid return statement. for instance:
			//{
			// {
			//   return x;
			// }
			//}
			// is legitimate.
			if (stmt instanceof StatementsBlock)
				returnFound = Boolean.valueOf(stmt.accept(this) + "");
			
		}
		
		return returnFound;
		
	}
	
	private boolean isPrimitive(IC.Semantics.Types.Type type) {
		return (type instanceof IC.Semantics.Types.PrimitiveType);
	}
	
	private boolean isVoid(IC.Semantics.Types.Type type) {
		return isPrimitive(type) &&
				((IC.Semantics.Types.PrimitiveType)type).isVoid();
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
		
		//of course only check methods here, no need to check fields...
		
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

	private void visitMethod(Method method, boolean isStatic) {

		//run this check for void methods as well, so we can detect
		//dead code after return statements. Just don't throw an
		//exception in case of "not all control paths return".
		
		//if return statement exists in out most scope, it does
		//not matter if and how many branches we have. At the
		//end, anything that didn't branch or branched and
		//didn't return inside branch, will reach the return
		//statement in the out most scope.
		//--> only check if there is no return statement found:
		
		//if we found a return statement in the out most method scope,
		//we're still going to run this check to see if there are branches
		//that return on all their control paths (which would make the return
		//statement we found a dead code).
		visitStatementBlockChildren = false;
		final boolean returnAtEndOfMethod = returnStatementExists(
				method.getStatements());
		boolean allPathsReturn = false;				
		visitStatementBlockChildren = true;
		
		for (Statement stmt : method.getStatements()) {

			if (allPathsReturn)
				throw new DeadCodeException("statement appearing after " +
					"return statement is unreachable.", stmt);
			
			Object stmtValue = stmt.accept(this);
			if (stmtValue != null) {
				//if returnAtEndOfMethod = true, we've found return at end
				//of statement block or at the end of the current method,
				//so don't update allPathsReturn (we've found the same thing
				//twice. Only update if we've checked an if statement).
				if (!returnAtEndOfMethod || (stmt instanceof If)) {
					allPathsReturn = Boolean.valueOf(stmtValue + "");
				}
			}
			
		}
		
		if (allPathsReturn && returnAtEndOfMethod) {
			throw new DeadCodeException("return statement " +
					"at end of method is unreachable.", method);
		}
		
		if (!isVoid(method.getNodeType()) && !allPathsReturn && !returnAtEndOfMethod) {
			throw new SemanticError("Non void " +
					(isStatic ? "static" : "virtual") + " method "
					+ "does not return a value on all of its' control paths",
					method);
		}

	}
	
	@Override
	public Object visit(VirtualMethod method) {
		visitMethod(method, false);
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		visitMethod(method, true);
		return null;
	}

	@Override
	public Object visit(LibraryMethod method) {
		//do nothing (no implementation, no return statements)
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
		//do nothing
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		//do nothing (we check using returnStatementExists if
		//exists, nothing to do for the actual visit)
		return null;
	}

	@Override
	public Object visit(If ifStatement) {

		//we're looking for a return statement on all control paths.
		//if the if statement has no else part, simply ignore it (return
		//false) because even if the if part is taken and returns on
		//every path, we're still not returning if branch wasn't taken.
		
		if (!ifStatement.hasElse())
			return false;
		
		boolean ifReturns = false;
		boolean elseReturns = false;
		
		ifReturns = (ifStatement.getOperation() instanceof Return);
		if (!ifReturns) {
			//only helpful if operation is block of statements!
			Object returnType = ifStatement.getOperation().accept(this);
			if (returnType != null) {
				ifReturns = Boolean.valueOf(returnType + "");
			}
		}
		
		elseReturns = (ifStatement.getElseOperation() instanceof Return);
		if (!elseReturns) {
			//only helpful if operation is block of statements!
			Object returnType = ifStatement.getElseOperation().accept(this);
			if (returnType != null) {
				elseReturns = Boolean.valueOf(returnType + "");
			}			
		}
		
		return (ifReturns && elseReturns);
	}

	@Override
	public Object visit(While whileStatement) {
		//do nothing
		
		//while branches are not important - if a branch is taken
		//and has a return statement on all of its' paths inside
		//a while, it still does not guarantee that if the while
		//branch wasn't taken, it will still return.
		
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
		
		//check if statement block has a return statement:
		boolean existsAtEndOfBlock = returnStatementExists(statementsBlock.getStatements());
		boolean allPathsReturn = false;
		
		//we did not find a return statement directly under the block,
		//so if we've exhausted the superficial search, we can go deeper
		//and look for if/else returns (actually go anyway to see if
		//if/else returns on all paths and the return we've found
		//at the end of the block is just dead code):
		if (visitStatementBlockChildren) {
			
			for (Statement stmt : statementsBlock.getStatements()) {

				if (allPathsReturn)
					throw new DeadCodeException("statement appearing after " +
						"return statement is unreachable.", stmt);
				
				Object stmtValue = stmt.accept(this);
				if (stmtValue != null) {
					//if returnAtEndOfBlock = true, we've found return at end
					//of statement block, either child of the current or the
					//current block itself,so don't update allPathsReturn
					//(we've found the same thing twice. Only update if we've checked an if statement).
					if (!existsAtEndOfBlock || (stmt instanceof If)) {
						allPathsReturn = Boolean.valueOf(stmtValue + "");
					}
				}
				
			}
			
			if (allPathsReturn && existsAtEndOfBlock) {
				throw new DeadCodeException("return statement " +
						"at end of method is unreachable.", statementsBlock);
			}
						
		}
		
		return existsAtEndOfBlock || allPathsReturn;
	}

	@Override
	public Object visit(LocalVariable localVariable) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(StaticCall call) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Length length) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		//do nothing
		return null;
	}

	
}
