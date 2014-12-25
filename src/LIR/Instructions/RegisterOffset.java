package LIR.Instructions;

import IC.AST.ASTNode;

public class RegisterOffset extends Operand {

	private final Register register;
	private final BasicOperand offset;
	
	public RegisterOffset(ASTNode node, Register register, BasicOperand offset) {
		super(node);
		this.register = register;
		this.offset = offset;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public Register getRegister() {
		return register;
	}

	public BasicOperand getOffset() {
		return offset;
	}
	
}
