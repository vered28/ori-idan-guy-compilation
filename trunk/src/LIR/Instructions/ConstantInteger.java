package LIR.Instructions;

import IC.AST.ASTNode;

public class ConstantInteger extends Immediate {

	private final int value;
	
	public ConstantInteger(ASTNode node, int value) {
		super(node);
		this.value = value;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public int getValue() {
		return value;
	}

}
