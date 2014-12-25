package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class Operand extends LIRInstruction {

	public Operand(ASTNode node) {
		super(node);
	}

}
