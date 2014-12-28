package IC.Semantics.Validations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import IC.LiteralTypes;
import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.Call;
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
import IC.Semantics.Exceptions.DeadCodeException;
import IC.Semantics.Exceptions.SemanticError;
import IC.Semantics.Scopes.BlockScope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.Symbol;

public class LocalVariableInitializedValidation implements Visitor {

	/* 
	 * This call checks that for every method, a local variable is only
	 * used after being initialized with a value.
	 * 
	 * Exceptions:
	 * 	- variable initialized with call statement (could be null, we don't
	 *    know, we don't care [at compile time], consider them initialized).
	 *  - new arrays. We know int arrays are initialized with zero,
	 *    but as far as all the others are, we don't care (though we
	 *    can safely assume they're initialized with nulls).
	 *  - other object's fields. If we have a local variable B b, we'll
	 *    see if it has gotten a value (B b = new B()), but we won't
	 *    (and can't) check if, for instance, b.x has been initialized.
	 *  - array out of bounds. new int[x] and then arr[x+10] is not checked
	 *    during compile-time.
	 */
	
	//keeps for every scope a set of symbols that were declared
	//in that scope, and initialized during branch executing
	//(meaning, where the execution is conditional).
	//this helps us solve cases like:
	//	int x;
	//	if (something)
	//		x = 3;
	//	x = x + 1;
	//where not all control paths initialized x.
	private Map<Scope, Set<Symbol>> initializedInBranches =
			new HashMap<Scope, Set<Symbol>>();

	//marks for each branch (if / else / while) whether or not it
	//contains a statements block (true) or a single instruction (false).
	private Stack<Boolean> branches = new Stack<Boolean>();
	
	//once we've seen a while or if/else, any values we might
	//have inside any variable that was assigned to, but not
	//necessarily initialized with them, is no longer reliable.
	private Map<Scope, Set<Symbol>> unreliableValues =
			new HashMap<Scope, Set<Symbol>>();
	
	private Set<Symbol> removeFromBranchInitilization(StatementsBlock statementsBlock) {
		
		Set<Symbol> assignedToInScope =
				unreliableValues.get(statementsBlock.getEnclosingScope());

		if (assignedToInScope != null) {
			//any variable that was assigned to within scope,
			//since we don't know if scope executed or if loop
			//then how many times, the value we have for it
			//is no longer reliable for other uses:
			for (Symbol symbol : assignedToInScope) {
				symbol.setValue(null);
			}
		}
		
		Set<Symbol> initiazliedInScope =
				initializedInBranches.get(statementsBlock.getEnclosingScope());
				
		if (initiazliedInScope != null) {
			
			//we initialized some variables inside the scope,
			//--> out side the scope (conditional scope), they
			//	  are not initialized :(
			// example:
			// int x;
			// while(something) { x = 4; }
			// x = x + 1; <-- if we didn't enter while, x is not initialized.
			
			for (Symbol symbol : initiazliedInScope) {
				symbol.setHasValue(false);
				symbol.setValue(null);
			}
			
			initializedInBranches.remove(statementsBlock.getEnclosingScope());
			
		}
		
		return initiazliedInScope;
	}
		
	private Object attemptToGetActualValue(Object val) {
		
		if (val instanceof Literal) {
			//we can't get anything more for strings (other than hasValue()).
			if (((Literal)val).getType() == LiteralTypes.INTEGER) {
				//we already checked really legal Java integer (signed 32-bits):
				return Integer.parseInt(((Literal)val).getValue() + "");
			} else if (((Literal)val).getType() == LiteralTypes.TRUE) {
				return true;
			} else if (((Literal)val).getType() == LiteralTypes.FALSE) {
				return false;
			}
		} else if (val instanceof Symbol) {
			if (((Symbol)val).hasValue())
				return ((Symbol) val).getValue();
		} else if (val instanceof Integer) {
			return val;
		} else if (val instanceof Boolean) {
			return val; //true or false
		}
		
		//give up :(
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

		//only checking local variables, don't visit fields
		
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
		//only checking local variables, don't visit formals
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		//only checking local variables, don't visit formals
		for (Statement stmt : method.getStatements()) {
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(LibraryMethod method) {
		//only checking local variables, library methods
		//don't have them --> do nothing
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
		
		Symbol var = (Symbol)assignment.getVariable().accept(this);
		Object value = assignment.getAssignment().accept(this);
		
		if (var == null) //assignment not for a local variable
			return null;

		if (!branches.isEmpty()) {
			
			boolean markAssignmentAsHazardous = false;			
			boolean singleInstructionOfBranch = !branches.peek();
			
			if (singleInstructionOfBranch) {
				//int x;
				//while (something)
				//	x = initWithValue();
				//hazard on x - only initialized on branch...
				markAssignmentAsHazardous = true;
			} else {
				
				boolean blockScopeBecauseOfCondition =
						((BlockScope)assignment.getEnclosingScope())
							.isScopeUnderCondition();
				
				if (assignment.getEnclosingScope() != var.getNode().getEnclosingScope()) {
					if (!blockScopeBecauseOfCondition) {
						//variable is defined in another scope.. this scope is not opened
						//because of condition, but because of programmer decision for
						//his / her personal convenience. Therefore, see if it is defined
						//in a parent scope, also not under condition
						BlockScope scope = (BlockScope)assignment.getEnclosingScope();
						while (var.getNode().getEnclosingScope() != scope
								&& !scope.isScopeUnderCondition()) {
							scope = (BlockScope)scope.getParentScope();
						}
						if (scope != var.getNode().getEnclosingScope()) {
							//variable is defined in completely different scope
							//(sepearted by a condition), and might be hazardous:
							//int x;
							//while(something)
							//{
							// {
							//  { 
						    //    x = 4;
						    //  }
							// }
							//}
							markAssignmentAsHazardous = true;
						}
					} else {
						//int x;
						//while(something) {
						//	x = 4;
						//}
						markAssignmentAsHazardous = true;
					}
					
				}
			}
			
			if (markAssignmentAsHazardous && !var.hasValue()) {
				if (!initializedInBranches.containsKey(assignment.getEnclosingScope())) {
					initializedInBranches.put(assignment.getEnclosingScope(),
							new HashSet<Symbol>());
				}
				initializedInBranches.get(assignment.getEnclosingScope()).add(var);
			}
			
			//mark this symbol as being unreliable after scope ends even if not
			//hazardous (it will still be initialized, just not reliable):
			if (var.hasValue()) {
				if (!unreliableValues.containsKey(assignment.getEnclosingScope())) {
					unreliableValues.put(assignment.getEnclosingScope(),
							new HashSet<Symbol>());
				}
				unreliableValues.get(assignment.getEnclosingScope()).add(var);
			}
		}
		
		var.setHasValue(true);
		var.setValue(value == null
				? null : attemptToGetActualValue(value));
		
		return var;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		callStatement.getCall().accept(this);
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		if (returnStatement.hasValue())
			return returnStatement.getValue().accept(this);
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visit(If ifStatement) {

		Object value = ifStatement.getCondition().accept(this);
		if (value != null) {
			value = attemptToGetActualValue(value);
			if (value != null && value instanceof Boolean) {
				boolean decision = ((Boolean)value);
				if (decision) {
					if (ifStatement.hasElse()) {
						throw new DeadCodeException("else part of if " +
								"statement is not reachable.", ifStatement);
					}
				} else {
					throw new DeadCodeException("if statement condition is " +
							"always false.", ifStatement);
				}
			}
		}
		
		/* ********************** if part of the if statement: **************/
		
		boolean singleInstruction =
				!(ifStatement.getOperation() instanceof StatementsBlock);
		
		if (!singleInstruction) {
			branches.push(true);
			((BlockScope)((StatementsBlock)ifStatement.getOperation())
					.getEnclosingScope()).setIsUnderCondition(true);
		} else {
			branches.push(false);
		}
		
		Object initializedInIf = ifStatement.getOperation().accept(this);
		
		if (!singleInstruction) {
			initializedInIf = removeFromBranchInitilization(
					(StatementsBlock)ifStatement.getOperation());
		} else {
			if (ifStatement.getOperation() instanceof Assignment) {
				if (initializedInIf != null) {

					((Symbol)initializedInIf).setValue(null);

					//if wasn't initialized before operation, also mark
					//it as uninitialized now:
					if (unreliableValues.containsKey(ifStatement.getEnclosingScope())
							&& !unreliableValues.get(ifStatement.getEnclosingScope())
								.remove(initializedInIf)) { //remove returns true only if element was in set
						((Symbol)initializedInIf).setHasValue(false);
					}

					//wrap in set:
					Symbol tmp = (Symbol)initializedInIf;
					initializedInIf = new HashSet<Symbol>();
					((HashSet<Symbol>)initializedInIf).add(tmp);
				}
			}
		}

		if (initializedInIf == null) {
			//init now to avoid null problems later.
			//this is good, it means we didn't init any
			//variable within if
			initializedInIf = new HashSet<Symbol>();
		}
		
		branches.pop();
		
		/* ********************** else part of the if statement: **************/

		if (ifStatement.hasElse()) {
			
			singleInstruction =
					!(ifStatement.getElseOperation() instanceof StatementsBlock);
			
			if (!singleInstruction) {
				branches.push(true);
				((BlockScope)((StatementsBlock)ifStatement.getElseOperation())
						.getEnclosingScope()).setIsUnderCondition(true);
			} else {
				branches.push(false);
			}
			
			Object initializedInElse = ifStatement.getElseOperation().accept(this);
			
			if (!singleInstruction) {
				initializedInElse = removeFromBranchInitilization(
						(StatementsBlock)ifStatement.getElseOperation());
			} else {
				if (ifStatement.getElseOperation() instanceof Assignment) {
					if (initializedInElse != null) {
						
						((Symbol)initializedInElse).setValue(null);

						//if wasn't initialized before operation, also mark
						//it as uninitialized now:
						if (unreliableValues.containsKey(ifStatement.getEnclosingScope())
								&& !unreliableValues.get(ifStatement.getEnclosingScope())
									.remove(initializedInElse)) { //remove returns true only if element was in set
							((Symbol)initializedInElse).setHasValue(false);
						}

						//wrap in set:
						Symbol tmp = (Symbol)initializedInElse;
						initializedInElse = new HashSet<Symbol>();
						((HashSet<Symbol>)initializedInElse).add(tmp);
					}
				}
			}
			
			if (initializedInElse == null) {
				//init now to avoid null problems later.
				//this is good, it means we didn't init any
				//variable within else
				initializedInElse = new HashSet<Symbol>();
			}
			
			branches.pop();

			/* **********************************************************
			 * 
			 * Compare between the variables initialized within if and
			 * those initialized within else. If some symbols agree, then
			 * they were initialized in both and can be considered
			 * initialized after both have run their course.
			 * 
			 * **********************************************************
			 */
			
			((HashSet<Symbol>)initializedInIf).retainAll(
					(HashSet<Symbol>)initializedInElse);
			
			//initializedInIf now contains only symbols common to both
			//initializedInIf and initializedInElse:
			
			for (Symbol symbol : (HashSet<Symbol>)initializedInIf) {
				symbol.setHasValue(true);
			}
			
		}
		
		return null;
	}

	@Override
	public Object visit(While whileStatement) {

		Object value = whileStatement.getCondition().accept(this);
		if (value != null) {
			value = attemptToGetActualValue(value);
			if (value != null && value instanceof Boolean) {
				if (!((Boolean)value)) {
					throw new DeadCodeException("while condition is always false",
							whileStatement);
				}
			}
		}
		
		boolean singleInstruction =
				!(whileStatement.getOperation() instanceof StatementsBlock);
		
		if (!singleInstruction) {
			branches.push(true);
			((BlockScope)((StatementsBlock)whileStatement.getOperation())
					.getEnclosingScope()).setIsUnderCondition(true);
		} else {
			branches.push(false);
		}
		
		Object val = whileStatement.getOperation().accept(this);
		
		if (!singleInstruction) {
			removeFromBranchInitilization(
					(StatementsBlock)whileStatement.getOperation());
		} else {
			if (whileStatement.getOperation() instanceof Assignment) {
				if (val != null) {
					
					((Symbol)val).setValue(null);
					
					//if wasn't initialized before operation, also mark
					//it as uninitialized now:
					if (unreliableValues.containsKey(whileStatement.getEnclosingScope())
							&& !unreliableValues.get(whileStatement.getEnclosingScope())
								.remove(val)) { //remove returns true only if element was in set
						((Symbol)val).setHasValue(false);
					}

				}
			}
		}
		
		branches.pop();		
		
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

		if (localVariable.hasInitValue()) {
			
			Object value = localVariable.getInitValue().accept(this);
			
			//find symbol for this local variable:
			Symbol sym = ScopesTraversal.findSymbol(
					localVariable.getName(), Kind.VARIABLE,
					localVariable.getEnclosingScope(),
					localVariable.getLine(), true);
			
			//sym != null since we've passed DeclarationValidation checks
			
			sym.setHasValue(true);
			sym.setValue(value == null ?
					null : attemptToGetActualValue(value));
			
		}
		
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {

		if (location.isExternal()) {
			if (location.getLocation() instanceof This) {
				//location refers to field or method -- ignore it
				return null;
			}
			return location.getLocation().accept(this);
		} else {
			
			//check if variable exists as Kind.Variable. Since we've
			//already checked every variable referenced has been
			//declared, if we get null, then it's just not a local
			//variable and therefore of no interest to us.
			
			Symbol var = ScopesTraversal.findSymbol(location.getName(),
					Kind.VARIABLE, location.getEnclosingScope(),
					location.getLine(),
					true /* no need to look outside the scope of the current
					        method, since we're only looking for a local variable*/
					);
			
			return var; //either null if no variable exists, or the variable
			            //symbol if one does exists.
			
		}
	}

	@Override
	public Object visit(ArrayLocation location) {
		
		Symbol array = (Symbol)location.getArray().accept(this);
		location.getIndex().accept(this);
		
		if (array == null) //array is not a local variable
			return null;
		
		//check that array has been initialized:
		if (!array.hasValue()) {
			throw new SemanticError("array " + array.getID()
					+ " has not been initialized.", location);
		}
		
		return array;
	}

	public void visitCall(Call call) {
		
		//we've already checked called method exists with the
		//right number of arguments and the correct types.
		//now let us check that the arguments sent to the
		//method have all been initialized.
		
		for (Expression expr : call.getArguments()) {
			expr.accept(this); //check legality of expression
		}
		
	}
	
	@Override
	public Object visit(StaticCall call) {
		visitCall(call);
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		visitCall(call);
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		// do nothing (cannot access local variables with this)
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		//do nothing (is the initValue() part of a local variable or
		//assignment, handled in their visits).
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {

		Object val = newArray.getSize().accept(this);
		if (val != null) {
			val = attemptToGetActualValue(val);
			if (val != null && val instanceof Integer) {
				if ((Integer)val <= 0)
					throw new SemanticError("array cannot be initialized" +
							" with non-positive value.", newArray);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(Length length) {
		length.getArray().accept(this);
		return null;
	}
	
	@Override
	public Object visit(MathBinaryOp binaryOp) {

		Object val1 = binaryOp.getFirstOperand().accept(this);
		Object val2 = binaryOp.getSecondOperand().accept(this);		

		if (val1 == null || val2 == null)
			return null; //at least one of them is not a local variable
				
		if (val1 instanceof Symbol && !((Symbol) val1).hasValue()) {
			throw new SemanticError("first operand is not initialized in"
					+ " math binary operation", binaryOp);
		} else if (val2 instanceof Symbol && !((Symbol) val2).hasValue()) {
			throw new SemanticError("second operand is not initialized in"
					+ " math binary operation", binaryOp);			
		}
		
		val1 = attemptToGetActualValue(val1);
		val2 = attemptToGetActualValue(val2);
		
		if (val1 == null || val2 == null)
			return null;
		
		if (val1 instanceof Integer && val2 instanceof Integer) {
			switch (binaryOp.getOperator()) {
				case PLUS:
					return (Integer)val1 + (Integer)val2;
				case MINUS:
					return (Integer)val1 - (Integer)val2;
				case MULTIPLY:
					return (Integer)val1 * (Integer)val2;
				case DIVIDE:
					if ((Integer)val2 == 0)
						throw new SemanticError("Division by zero exception", binaryOp);
					return (Integer)val1 / (Integer)val2;
				case MOD:
					return (Integer)val1 % (Integer)val2;
				default:
					//do nothing
			}
		}
		
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {

		Object val1 = binaryOp.getFirstOperand().accept(this);
		Object val2 = binaryOp.getSecondOperand().accept(this);		

		if (val1 == null || val2 == null)
			return null; //at least one of them is not a local variable

		if (val1 instanceof Symbol && !((Symbol) val1).hasValue()) {
			throw new SemanticError("first operand is not initialized in"
					+ " logical binary operation", binaryOp);
		} else if (val2 instanceof Symbol && !((Symbol) val2).hasValue()) {
			throw new SemanticError("second operand is not initialized in"
					+ " logical binary operation", binaryOp);			
		}

		val1 = attemptToGetActualValue(val1);
		val2 = attemptToGetActualValue(val2);
		
		if (val1 == null || val2 == null)
			return null;
		
		boolean bothInts = (val1 instanceof Integer && val2 instanceof Integer);
		boolean bothBools = (val1 instanceof Boolean && val2 instanceof Boolean);
		
		if (!bothInts && !bothBools)
			return null; //we can't do anything helpful here...
		
		String operator = binaryOp.getOperator().getOperatorString();
		
		if (operator.equals("==")) {
			if (bothInts)
				return ((Integer)val1 == (Integer)val2);
			if (bothBools)
				return ((Boolean)val1 == (Boolean)val2);
		} else if (operator.equals("!=")) {
			if (bothInts)
				return ((Integer)val1 != (Integer)val2);
			if (bothBools)
				return ((Boolean)val1 != (Boolean)val2);			
		} else if (operator.equals("<")) {
			if (bothInts)
				return ((Integer)val1 < (Integer)val2);
		} else if (operator.equals("<=")) {
			if (bothInts)
				return ((Integer)val1 <= (Integer)val2);
		} else if (operator.equals(">")) {
			if (bothInts)
				return ((Integer)val1 > (Integer)val2);
		} else if (operator.equals(">=")) {
			if (bothInts)
				return ((Integer)val1 >= (Integer)val2);
		} else if (operator.equals("&&")) {
			if (bothBools)
				return ((Boolean)val1 && (Boolean)val2);
		} else if (operator.equals("||")) {
			if (bothBools)
				return ((Boolean)val1 || (Boolean)val2);
		}
		
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		
		Object val = unaryOp.getOperand().accept(this);

		if (val == null)
			return null; //not a local variable

		if (val instanceof Symbol && !((Symbol)val).hasValue()) {
			throw new SemanticError("operand is not initialized in"
					+ " math unary operation", unaryOp);
		}

		val = attemptToGetActualValue(val);
		
		if (val == null)
			return null;
		
		if (val instanceof Integer)
			return -((Integer)val);
		
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		
		Object val = unaryOp.getOperand().accept(this);

		if (val == null)
			return null; //not a local variable
		
		if (val instanceof Symbol && !((Symbol)val).hasValue()) {
			throw new SemanticError("operand is not initialized in"
					+ " logical unary operation", unaryOp);
		}

		val = attemptToGetActualValue(val);
		
		if (val == null)
			return null;
		
		if (val instanceof Boolean)
			return !((Boolean)val);
		
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		return literal;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		return expressionBlock.getExpression().accept(this);
	}

}
