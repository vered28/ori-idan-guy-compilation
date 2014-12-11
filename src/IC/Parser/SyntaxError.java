package IC.Parser;

public class SyntaxError extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String type;
	private int line;
	private int column;
	
	public SyntaxError(String msg, String type, int line, int column) {
		super(msg);
		this.type = type;
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
