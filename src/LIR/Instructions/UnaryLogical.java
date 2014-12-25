package LIR.Instructions;

import IC.AST.ASTNode;
import LIR.UnaryOps;

public class UnaryLogical extends UnaryInstruction {

	private final UnaryOps op = UnaryOps.Not;
	
	public UnaryLogical(ASTNode node, Register operand) {
		super(node, operand);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public UnaryOps getOperation() {
		return op;
	}

}
