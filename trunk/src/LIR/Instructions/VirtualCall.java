package LIR.Instructions;

import java.util.HashMap;
import java.util.Map;

import IC.AST.ASTNode;

public class VirtualCall extends Call {

	private final RegisterOffset offset;
	private Map<Memory, BasicOperand> params;
	
	public VirtualCall(ASTNode node, RegisterOffset offset, Register returnRegister) {
		super(node, returnRegister);
		this.offset = offset;
		this.params = new HashMap<Memory, BasicOperand>();
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public RegisterOffset getOffset() {
		return offset;
	}
	
	public BasicOperand getParameter(Memory mem) {
		return params.get(mem);
	}
	
	public Map<Memory, BasicOperand> getParameters() {
		return params;
	}
	
	public void addParameter(Memory mem, BasicOperand param) {
		params.put(mem, param);
	}

}
