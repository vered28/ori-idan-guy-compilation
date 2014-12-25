package LIR.Instructions;

import IC.AST.ASTNode;

/** LIR refers to variables by the name "Memory".
 *  (This does NOT mean main memory access!)
 */
public class Memory extends BasicOperand {

	private final String variableName;
	
	public Memory(ASTNode node, String variableName) {
		super(node);
		this.variableName = variableName;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public String getVariableName() {
		return variableName;
	}

}
