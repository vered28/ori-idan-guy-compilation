package LIR.Instructions;

import IC.AST.ASTNode;

public class ArrayLoad extends MoveArray {

	public ArrayLoad(ASTNode node, ArrayLocation operand1, Register operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
