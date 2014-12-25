package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class BinaryInstruction extends LIRInstruction {

	private final Operand operand1;
	private final Operand operand2;
	
	public BinaryInstruction(ASTNode node, Operand operand1, Operand operand2) {
		super(node);
		this.operand1 = operand1;
		this.operand2 = operand2;
	}
	
	public Operand getFirstOperand() {
		return operand1;
	}
	
	public Operand getSecondOperand() {
		return operand2;
	}
	
}
