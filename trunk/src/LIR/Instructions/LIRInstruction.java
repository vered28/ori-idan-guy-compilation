package LIR.Instructions;

import IC.AST.ASTNode;

public abstract class LIRInstruction {

	private ASTNode associatedICNode;
	
	public LIRInstruction(ASTNode node) {
		this.associatedICNode = node;
	}
	
	public ASTNode getAssociactedICNode() {
		return associatedICNode;
	}
	
	public abstract Object accept(LIRVisitor visitor);
	
}
