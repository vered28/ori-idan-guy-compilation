package LIR.Instructions;

import IC.AST.ASTNode;

public class ArrayLength extends DataTransferInstruction {

	/**
	 * ArrayLength instruction for doing R1.length for array R1
	 * Or str1.length for string constant str1.
	 *
	 * @param operand1 array/string address
	 * @param operand2 register to store the returned length (result) in
	 */
	public ArrayLength(ASTNode node, BasicOperand operand1, Register operand2) {
		super(node, operand1, operand2);
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}

}
