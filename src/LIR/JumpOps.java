package LIR;

public enum JumpOps {

	Jump("Jump"),
	JumpTrue("JumpTrue"),
	JumpFalse("JumpFalse"),
	JumpGT("JumpG"),
	JumpGE("JumpGE"),
	JumpLT("JumpL"),
	JumpLE("JumpLE");
	
	private String description;
	
	private JumpOps(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
}
