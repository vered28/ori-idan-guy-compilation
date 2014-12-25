package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class UnaryInstruction extends LIRInstruction {

	private final Register operand;
	
	public UnaryInstruction(ASTNode node, Register operand) {
		super(node);
		this.operand = operand;
	}
	
	public Register getOperand() {
		return operand;
	}

}
