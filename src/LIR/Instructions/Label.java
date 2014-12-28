package LIR.Instructions;

import IC.AST.ASTNode;

public class Label extends LIRInstruction {
	
	private final String value;

	public Label(ASTNode node, String value) {
		super(node);
		this.value = value;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public String getValue() {
		return value;
	}

}
