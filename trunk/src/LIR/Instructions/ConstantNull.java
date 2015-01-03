package LIR.Instructions;

import IC.AST.ASTNode;

public class ConstantNull extends ConstantInteger {

	public ConstantNull(ASTNode node) {
		super(node, 0);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
