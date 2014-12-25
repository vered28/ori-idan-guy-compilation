package LIR.Translation;

import IC.AST.Literal;

public class StringLiteral {

	//use long so the chances of overflowing and not supporting
	//massive programs is even less likely than with an integer.
	private static long counter = 0;
	
	private Literal literal;
	private String id;
	
	public StringLiteral(Literal literal) {
		this.literal = literal;
		this.id = ("str" + ++counter);
	}
	
	public Literal getLiteral() {
		return literal;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof StringLiteral)) return false;
		
		//determine equality by strings themselves:
		return (((StringLiteral)obj).getLiteral().getValue() + "")
				.equals(literal.getValue() + "");
	}
}
