package LIR.Instructions;

import IC.AST.ASTNode;

public class ConstantString extends Immediate {

	private final String label;
	
	public ConstantString(ASTNode node, String label) {
		super(node);
		this.label = label;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public String getLabel() {
		return label;
	}

}
