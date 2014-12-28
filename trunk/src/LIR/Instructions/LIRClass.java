package LIR.Instructions;

import java.util.List;

import IC.AST.ASTNode;

public class LIRClass extends LIRInstruction {
	
	private final LIRClass superClass;
	
	private List<LIRMethod> methods;
	
	public LIRClass(ASTNode node, List<LIRMethod> methods) {
		this(node, null, methods);
	}

	public LIRClass(ASTNode node, LIRClass superClass, List<LIRMethod> methods) {
		super(node);
		this.superClass = superClass;
		this.methods = methods;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public LIRClass getSuperClass() {
		return superClass;
	}
	
	public List<LIRMethod> getMethods() {
		return methods;
	}
}
