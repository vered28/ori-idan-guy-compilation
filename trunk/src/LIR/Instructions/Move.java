package LIR.Instructions;

import IC.AST.ASTNode;

public class Move extends DataTransferInstruction {

	public Move(ASTNode node, Operand operand1, Operand operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
