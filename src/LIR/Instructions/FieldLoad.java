package LIR.Instructions;

import IC.AST.ASTNode;

public class FieldLoad extends MoveField {

	public FieldLoad(ASTNode node, RegisterOffset operand1, Register operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
