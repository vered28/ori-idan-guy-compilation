package LIR.Instructions;

import IC.AST.ASTNode;

public class FieldStore extends MoveField {

	public FieldStore(ASTNode node, BasicOperand operand1, RegisterOffset operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
