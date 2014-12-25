package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class MoveField extends DataTransferInstruction {

	public MoveField(ASTNode node, Operand operand1, Operand operand2) {
		super(node, operand1, operand2);
	}

}
