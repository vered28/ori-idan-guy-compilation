package LIR.Instructions;

import IC.AST.ASTNode;

public class ArrayStore extends MoveArray {

	public ArrayStore(ASTNode node, BasicOperand operand1, ArrayLocation operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
