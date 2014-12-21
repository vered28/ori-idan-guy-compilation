package IC.Semantics.Exceptions;

import IC.AST.ASTNode;

public class SemanticError extends RuntimeException {
	
	private static final long serialVersionUID = 7375323872600029402L;
	private final String type = "semantic error";
	private int line;
	private int column;
	
	public SemanticError(String msg) {
		super(msg);
	}
	
	public SemanticError(String msg, int line, int column) {
		super(msg);
		this.line = line;
		this.column = column;
	}
	
	public SemanticError(String message, ASTNode node) {
		this(message, node.getLine(), node.getColumn());
	}

	public String getType() {
		return type;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

}
