package IC.AST;

import IC.Semantics.Scopes.Scope;
import IC.Semantics.Types.Type;

/**
 * Abstract AST node base class.
 * 
 * @author Tovi Almozlino
 */
public abstract class ASTNode {

	private int line;
	private int column;
	private Scope enclosingScope;
	private Type nodeType;

	/**
	 * Double dispatch method, to allow a visitor to visit a specific subclass.
	 * 
	 * @param visitor
	 *            The visitor.
	 * @return A value propagated by the visitor.
	 */
	public abstract Object accept(Visitor visitor);

	/**
	 * Constructs an AST node corresponding to a line number in the original
	 * code. Used by subclasses.
	 * 
	 * @param line
	 *            The line number.
	 */
	protected ASTNode(int line, int column) {
		this.line = line;
		this.column = column; 
	}

	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
	
	public void setEnclosingScope(Scope scope) {
		enclosingScope = scope;
	}
	
	public Scope getEnclosingScope() {
		return enclosingScope;
	}
	
	public Type getNodeType() {
		return nodeType;
	}
	
	public void setNodeType(Type nodeType) {
		this.nodeType = nodeType;
	}

}
