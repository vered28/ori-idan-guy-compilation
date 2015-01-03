package LIR;

public enum UnaryOps {

	Inc("Inc"),
	Dec("Dec"),
	Neg("Neg"),
	Not("Not");
	
	private String description;
	
	private UnaryOps(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
}
