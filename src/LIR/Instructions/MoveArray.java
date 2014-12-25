package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class MoveArray extends DataTransferInstruction {

	public MoveArray(ASTNode node, Operand operand1, Operand operand2) {
		super(node, operand1, operand2);
	}

}
