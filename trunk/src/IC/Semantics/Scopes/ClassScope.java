package IC.Semantics.Scopes;

import IC.AST.ASTNode;

public class ClassScope extends Scope {

	public ClassScope(String id, ASTNode node) {
		this(id, null, node);
	}

	public ClassScope(String id, Scope parent, ASTNode node) {
		super(id, parent);
		
		//add this to scope and ignore exception (cannot be thrown, nothing
		//has been added yet):
		try {
			addToScope(new Symbol("this", Type.THIS, Kind.CLASS, node));
		} catch (Exception e) {
			//ignore / do nothing
		}
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

}
