package LIR.Instructions;

import IC.AST.ASTNode;

public class LIRClass extends LIRInstruction {
	
	private final LIRClass superClass;
	
	public LIRClass(ASTNode node) {
		this(node, null);
	}

	public LIRClass(ASTNode node, LIRClass superClass) {
		super(node);
		this.superClass = superClass;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public LIRClass getSuperClass() {
		return superClass;
	}
}
