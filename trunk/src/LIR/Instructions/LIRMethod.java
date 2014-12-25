package LIR.Instructions;

import java.util.LinkedList;
import java.util.List;

import IC.AST.ASTNode;

public class LIRMethod extends LIRInstruction {

	private List<LIRInstruction> instructions;
	
	public LIRMethod(ASTNode node) {
		super(node);
		this.instructions = new LinkedList<LIRInstruction>();
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public List<LIRInstruction> getInstructions() {
		return instructions;
	}
	
	public void addInstruction(LIRInstruction inst) {
		instructions.add(inst);
	}

}
