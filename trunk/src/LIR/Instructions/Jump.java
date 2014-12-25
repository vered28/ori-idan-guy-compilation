package LIR.Instructions;

import IC.AST.ASTNode;
import LIR.JumpOps;

public class Jump extends LIRInstruction {

	private final JumpOps op;
	private final String JumpToLabel;
	
	public Jump(ASTNode node, JumpOps op, String label) {
		super(node);
		this.op = op;
		this.JumpToLabel = label;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public JumpOps getOperation() {
		return op;
	}
	
	public String getLabel() {
		return JumpToLabel;
	}

}
