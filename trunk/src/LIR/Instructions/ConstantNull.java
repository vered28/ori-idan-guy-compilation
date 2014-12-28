package LIR.Instructions;

import IC.AST.ASTNode;

public class ConstantNull extends Immediate {

	public ConstantNull(ASTNode node) {
		super(node);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
