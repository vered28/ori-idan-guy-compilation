package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class Call extends LIRInstruction {

	//Register that result will be written to
	private Register returnRegister;
	
	public Call(ASTNode node, Register returnRegister) {
		super(node);
		this.returnRegister = returnRegister;
	}
	
	public void setReturnRegister(Register register) {
		this.returnRegister = register;
	}
	
	public Register getReturnRegister() {
		return returnRegister;
	}

}
