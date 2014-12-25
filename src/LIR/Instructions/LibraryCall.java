package LIR.Instructions;

import java.util.LinkedList;
import java.util.List;

import IC.AST.ASTNode;

public class LibraryCall extends Call {

	private final String funcName;
	private List<BasicOperand> params;
	
	public LibraryCall(ASTNode node, String funcName, Register returnRegister) {
		super(node, returnRegister);
		this.funcName = funcName;
		this.params = new LinkedList<BasicOperand>();
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public String getName(){
		return funcName;
	}
	
	public List<BasicOperand> getParameters() {
		return params;
	}
	
	public void addParameter(BasicOperand param) {
		params.add(param);
	}

}
