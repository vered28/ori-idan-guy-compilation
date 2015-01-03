package LIR.Instructions;

import IC.AST.ASTNode;
import LIR.UnaryOps;

public class UnaryArithmetic extends UnaryInstruction {
	
	private final UnaryOps op;
	
	public UnaryArithmetic(ASTNode node, UnaryOps op, Register operand) {
		super(node, operand);
		this.op = op;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public UnaryOps getOperation() {
		return op;
	}

}
