package LIR.Instructions;

import IC.AST.ASTNode;
import LIR.BinaryOps;

public class BinaryArithmetic extends BinaryInstruction {

	private final BinaryOps op;
	
	public BinaryArithmetic(ASTNode node, BinaryOps op,
			BasicOperand operand1, Register operand2) {
		super(node, operand1, operand2);
		this.op = op;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public BinaryOps getOperation() {
		return op;
	}

}
