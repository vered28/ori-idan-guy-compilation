package IC.Parser;

import java_cup.runtime.Symbol;

public class Token extends Symbol {

	private int line, column;
	private String tag, value;

	public Token(int id, int line) {
		super(id, null);
	}

	public Token(int id, int line, int column, String tag, String value) {
		super(id, value);
		this.line = line;
		this.column = column;
		this.tag = tag;
		this.value = value;
	}

	public int getID() {
		return sym;
	}
	
	public void setLine(int line) {
		this.line = line;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public String getTag() {
		return tag;
	}

	public String getValue() {
		return value;
	}
}
