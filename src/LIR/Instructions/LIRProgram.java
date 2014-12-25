package LIR.Instructions;

import java.util.LinkedList;
import java.util.List;

import IC.AST.ASTNode;

public class LIRProgram extends LIRInstruction {

	private List<LIRClass> classes;
	
	public LIRProgram(ASTNode node) {
		super(node);
		this.classes = new LinkedList<LIRClass>();
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public void addClass(LIRClass lirclass) {
		classes.add(lirclass);
	}
	
	public List<LIRClass> getClasses() {
		return classes;
	}

}
