package LIR.Instructions;

import IC.AST.ASTNode;

public class Return extends LIRInstruction {

	private final BasicOperand value;
	
	public Return(ASTNode node, BasicOperand value) {
		super(node);
		this.value = value;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public BasicOperand getValue() {
		return value;
	}

}
