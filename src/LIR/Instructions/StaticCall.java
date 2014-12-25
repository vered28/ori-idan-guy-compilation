package LIR.Instructions;

import java.util.HashMap;
import java.util.Map;

import IC.AST.ASTNode;

public class StaticCall extends Call {

	private final String funcName;
	private Map<Memory, BasicOperand> params;
	
	public StaticCall(ASTNode node, String funcName, Register returnRegister) {
		super(node, returnRegister);
		this.funcName = funcName;
		this.params = new HashMap<Memory, BasicOperand>();
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public String getName() {
		return funcName;
	}
	
	public BasicOperand getParameter(Memory mem) {
		return params.get(mem);
	}
	
	public void addParameter(Memory mem, BasicOperand param) {
		params.put(mem, param);
	}

}
