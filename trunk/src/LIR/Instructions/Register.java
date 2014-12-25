package LIR.Instructions;

import IC.AST.ASTNode;

public class Register extends BasicOperand {

	//for R1, for example, num = 1; for R2, num = 2; and so on
	private final int num;
	
	public Register(ASTNode node, int num) {
		super(node);
		this.num = num;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public int getNum() {
		return num;
	}

}
