package IC.Semantics.Scopes;

import java.util.HashMap;
import java.util.Map;

import IC.AST.ASTNode;
import IC.AST.Expression;
import IC.AST.Method;
import IC.AST.Statement;
import IC.Semantics.Types.Type;

public class ExtendedSymbol extends Symbol {

	private Map<Method, Statement> lastStatementUsed = null;
	private Map<Method, Expression> lastExpressionUsed = null;

	public ExtendedSymbol(String id, Type type, Kind kind, ASTNode node) {
		super(id, type, kind, node);
		
		lastStatementUsed = new HashMap<Method, Statement>();
		lastExpressionUsed = new HashMap<Method, Expression>();
	}
	
	@Override
	public Statement getLastStatementUsed() {
		throw new UnsupportedOperationException();
	}
	
	public void setLastStatementUsed(Method method, Statement stmt) {
		if (stmt == null)
			lastStatementUsed.remove(method);
		else
			lastStatementUsed.put(method, stmt);
	}
	public void setLastExpressionUsed(Method method, Expression expr) {
		if (expr == null)
			lastExpressionUsed.remove(method);
		else
			lastExpressionUsed.put(method, expr);
	}
	
	public Statement getLastStatementUsed(Method method) {
		return lastStatementUsed.get(method);
	}

	public Expression getLastExpressionUsed(Method method) {
		return lastExpressionUsed.get(method);
	}

}
