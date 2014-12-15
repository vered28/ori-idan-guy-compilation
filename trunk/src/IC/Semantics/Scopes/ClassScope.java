package IC.Semantics.Scopes;

import IC.AST.ASTNode;

public class ClassScope extends Scope {

	public ClassScope(String id, ASTNode node) {
		this(id, null, node);
	}

	public ClassScope(String id, Scope parent, ASTNode node) {
		super(id, parent);
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

}
