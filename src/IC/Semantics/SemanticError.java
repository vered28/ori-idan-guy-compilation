package IC.Semantics;

public class SemanticError extends RuntimeException {
	
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
