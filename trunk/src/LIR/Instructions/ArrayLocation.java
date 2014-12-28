package LIR.Instructions;

import IC.AST.ASTNode;

public class ArrayLocation extends Operand {

	private final Register array;
	private final BasicOperand location;
	
	public ArrayLocation(ASTNode node, Register array, BasicOperand location) {
		super(node);
		this.array = array;
		this.location = location;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public Register getArray() {
		return array;
	}
	
	public BasicOperand getLocation() {
		return location;
	}

}
