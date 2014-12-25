package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class Call extends LIRInstruction {

	//Register that result will be written to
	private final Register returnRegister;
	
	public Call(ASTNode node, Register returnRegister) {
		super(node);
		this.returnRegister = returnRegister;
	}
	
	public Register getReturnRegister() {
		return returnRegister;
	}

}
