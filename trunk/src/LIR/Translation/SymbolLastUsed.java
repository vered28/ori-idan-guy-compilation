package LIR.Translation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import IC.Semantics.Scopes.ExtendedSymbol;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;

public class SymbolLastUsed implements Visitor {

	/* This class assigns each symbol in method or block a statement
	 * (and sometimes expression) where it is last used (=read).
	 * This will help us know when translating to LIR when
	 * registers are "dead" and can be reused. */
	
	private ICClass currentClass = null;
	private Method currentMethod = null;
	private Statement currentStatement = null;
	private Stack<Expression> currentExpression = new Stack<Expression>();
	
	//look for instance at the following code:
	//while (x < 10)
	//	x = x + y;
	//the assignment is a later statement in which x is used,
	//but in reality the condition will always be the last
	//expression / statement to be executed, therefore we
	//must mark x variable as being last used by the condition!
	//also, y is used in every iteration, don't mark it as last
	//used by the assignment, but by the loop!:
	private Set<Symbol> symbolsUsedInCurrentWhileLoop = null;
	
	private boolean whileCondition = false;

	//while loops may be nested:
	private Map<Symbol, Integer> symbolsUsedInWhileLoop = new HashMap<Symbol, Integer>();
	private Stack<Boolean> whileLoop = new Stack<Boolean>();
	
	//first basic operand to appear in while condition, if never
	//used before the condition, will be taken as memory value,
	//not register value. So don't mark it as being used (since
	//not loaded to register):
	private boolean considerAsUsedIfAlreadyUsed = false;
	
	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine) {
		return findSymbol(id, kinds, scope, aboveLine, false);
	}

	private Symbol findSymbol(String id, List<Kind> kinds, Scope scope, int aboveLine, boolean onlyCheckMethodScope) {
		
		Symbol symbol;
		for (Kind k : kinds) {
			if ((symbol = ScopesTraversal.findSymbol(id, k, scope, aboveLine, onlyCheckMethodScope)) != null)
				return symbol;
		}

		return null;
	}

	@Override
	public Object visit(Program program) {
		for (ICClass cls : program.getClasses()) {
			currentClass = cls;
			cls.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {
		for (Method method : icClass.getMethods()) {
			currentMethod = method;
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
				
		//store existing symbols in set:
		//		while (x) while(y) { ... }
		//we are now in while(y), and need to preserve symbolsUsedInCurrentWhileLoop for
		//while(x) loop, which may not have finished yet (there could be some code
		//after inner loop finishes)
		Set<Symbol> outerWhileSymbols = null;
		if (symbolsUsedInCurrentWhileLoop != null)
			outerWhileSymbols = new HashSet<Symbol>(symbolsUsedInCurrentWhileLoop);

		//initialize set for current loop:
		symbolsUsedInCurrentWhileLoop = new HashSet<Symbol>();
		
		//while inside loop, don't mark symbols existing in
		//symbolsUsedInWhileLoop as used:
		whileLoop.push(true);

		whileCondition = true;
		whileStatement.getCondition().accept(this);
		whileCondition = false;
				
		//run operation:
		whileStatement.getOperation().accept(this);
		
		//mark all symbols used as last used by while:
		for (Symbol symbol : (outerWhileSymbols == null ? symbolsUsedInWhileLoop.keySet() : symbolsUsedInCurrentWhileLoop)) {
			
			int count = symbolsUsedInWhileLoop.get(symbol);
			if (count > 0)
				symbolsUsedInWhileLoop.put(symbol, count-1);
						
			if (symbol instanceof ExtendedSymbol) {
				ExtendedSymbol extended = (ExtendedSymbol)symbol;
				extended.setLastStatementUsed(currentMethod, whileStatement);
				extended.setLastExpressionUsed(currentMethod, null);
			} else {
				symbol.setLastStatementUsed(whileStatement);
				symbol.setLastExpressionUsed(null);
			}
		}
				
		whileLoop.pop();
		
		//restore outer loop symbols:
		if (outerWhileSymbols != null)
			symbolsUsedInCurrentWhileLoop = new HashSet<Symbol>(outerWhileSymbols);
		else {
			symbolsUsedInCurrentWhileLoop = null;
			symbolsUsedInWhileLoop.clear();
		}
			
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
		
		if (location.isExternal() && !(location.getLocation() instanceof This)) {
			//don't worry about internals of externals...
			//in order to access another object's fields, we'd have
			//to have the object itself in use (i.e A a = new A(); a.field = x; )
			//worry only that a is used.
			location.getLocation().accept(this);
		} else {
		
			Symbol sym = findSymbol(location.getName(),
							Arrays.asList(new Kind[] { Kind.VARIABLE, Kind.FORMAL, Kind.FIELD }),
							location.getEnclosingScope(),
							location.getLine());
			
			if (sym != null) {
				
				//only mark this statement / expression as last using the local
				//variable if not inside a while loop or if inside a while loop
				//but symbol is not used in while condition:
				//(or it is marked to ignore for consideration):
				boolean used = ((sym instanceof ExtendedSymbol &&
						((ExtendedSymbol)sym).getLastStatementUsed(currentMethod) != null)
						|| (!(sym instanceof ExtendedSymbol) &&sym.getLastStatementUsed() != null));
				
				//check if we need DV_PTR (using field that wasn't used before)
				boolean unusedFieldAccess = (sym.getKind() == Kind.FIELD && !used);
				ExtendedSymbol classSymbol = null;
				if (unusedFieldAccess) {
					 classSymbol = (ExtendedSymbol)
							ScopesTraversal.findSymbol(
							currentClass.getName(),
							Kind.CLASS, location.getEnclosingScope());
				}

				
				if ((whileLoop.isEmpty() ||
						(!whileLoop.isEmpty() && !symbolsUsedInWhileLoop.containsKey(sym))) &&
						(!whileCondition ||
								(whileCondition && (!considerAsUsedIfAlreadyUsed ||
										(considerAsUsedIfAlreadyUsed && used))))) {						
										
					if (sym instanceof ExtendedSymbol) {

						ExtendedSymbol extended = (ExtendedSymbol)sym;
						extended.setLastStatementUsed(currentMethod, currentStatement);
						if (!currentExpression.isEmpty()) {
							extended.setLastExpressionUsed(currentMethod, currentExpression.peek());
						} else {
							//currentStatement is more advanced than expression,
							//last expression used is residue from old usage,
							//remove it:
							extended.setLastExpressionUsed(currentMethod, null);
						}

					} else {
					
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
					
					if (classSymbol != null) {
						classSymbol.setLastStatementUsed(currentMethod, currentStatement);
						if (!currentExpression.isEmpty()) {
							classSymbol.setLastExpressionUsed(currentMethod, currentExpression.peek());
						} else {
							classSymbol.setLastExpressionUsed(currentMethod, null);
						}
					}
					
				}
				
				//mark symbols within loop condition or operation (so their last statement
				//used will be currentWhileLoop), unless their first operand of condition,
				//in which case they won't be registered at this point, so don't save it
				//as last used (see translation to understand why first operand is special)
				if (!whileLoop.isEmpty() && 
						(!considerAsUsedIfAlreadyUsed ||
								(considerAsUsedIfAlreadyUsed && used))) {

					Symbol[] symbols = (classSymbol == null ?
							new Symbol[] { sym } :
								new Symbol[] { sym, classSymbol });
					
					for (Symbol symbol : symbols) {
					
						symbolsUsedInCurrentWhileLoop.add(symbol);
						
						if (symbolsUsedInWhileLoop.containsKey(symbol)) {
							int count = symbolsUsedInWhileLoop.get(symbol);
							symbolsUsedInWhileLoop.put(symbol, count+1);
						} else {
							symbolsUsedInWhileLoop.put(symbol, 1);
						}
						
					}
					
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
		
		if (call.isExternal()) {
			//don't worry about internals of externals:
			//a.foo()
			//just need to check if a is used or not
			call.getLocation().accept(this);
		} else {
		
			 ExtendedSymbol classSymbol = (ExtendedSymbol)
						ScopesTraversal.findSymbol(
						currentClass.getName(),
						Kind.CLASS, call.getEnclosingScope());
			 
			 if (whileLoop.isEmpty()) {
				 //not inside loop:
				 classSymbol.setLastStatementUsed(currentMethod, currentStatement);
				 classSymbol.setLastExpressionUsed(currentMethod, call);
			 } else {
				 
				symbolsUsedInCurrentWhileLoop.add(classSymbol);
					
				if (symbolsUsedInWhileLoop.containsKey(classSymbol)) {
					int count = symbolsUsedInWhileLoop.get(classSymbol);
					symbolsUsedInWhileLoop.put(classSymbol, count+1);
				} else {
					symbolsUsedInWhileLoop.put(classSymbol, 1);
				}

			 }
		}

		
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
		considerAsUsedIfAlreadyUsed =
				(!considerAsUsedIfAlreadyUsed && whileCondition);
		binaryOp.getFirstOperand().accept(this);
		considerAsUsedIfAlreadyUsed = false;
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
		considerAsUsedIfAlreadyUsed = 
				(!considerAsUsedIfAlreadyUsed && whileCondition);
		unaryOp.getOperand().accept(this);
		considerAsUsedIfAlreadyUsed = false;
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
