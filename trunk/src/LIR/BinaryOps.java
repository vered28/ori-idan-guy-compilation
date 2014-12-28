package LIR;

public enum BinaryOps {

	Add("Add"),
	Sub("Sub"),
	Mul("Mul"),
	Div("Div"),
	Mod("Mod"),
	And("And"),
	Or("Or"),
	Xor("Xor"),
	Compare("Compare");
	
	private String description;
	
	private BinaryOps(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
}
