package LIR.Instructions;

import java.util.LinkedList;
import java.util.List;

import IC.AST.ASTNode;

public class LIRClass extends LIRInstruction {

	//TODO: add DVPTR, fields and offsets for fields and methods
	private List<LIRMethod> methods;
	
	private final LIRClass superClass;
	
	public LIRClass(ASTNode node) {
		this(node, null);
	}

	public LIRClass(ASTNode node, LIRClass superClass) {
		super(node);
		this.methods = new LinkedList<LIRMethod>();
		this.superClass = superClass;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public List<LIRMethod> getMethods() {
		return methods;
	}
	
	public void addMethod(LIRMethod method) {
		methods.add(method);
	}
	
	public LIRClass getSuperClass() {
		return superClass;
	}
}
