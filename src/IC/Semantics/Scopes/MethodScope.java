package IC.Semantics.Scopes;

public class MethodScope extends ClassScope {

	public MethodScope(String id) {
		super(id);
	}

	public MethodScope(String id, Scope parent) {
		super(id, parent);
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

}
