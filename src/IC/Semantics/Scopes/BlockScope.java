package IC.Semantics.Scopes;

public class BlockScope extends MethodScope {

	public BlockScope(Scope parent) {
		super("statement block in " + parent.getID(), parent);
	}

	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}
}
