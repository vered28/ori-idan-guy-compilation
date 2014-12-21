package IC.Semantics.Scopes;

public class BlockScope extends MethodScope {

	/* when true, block scope was opened to allow multiple
	 * statements under if / else / while.
	 * when false, marks that block scope was done for no
	 * other reason than sheer convenience.
	 * 
	 */
	private boolean scopeUnderCondition = false;
	
	public BlockScope(Scope parent) {
		super("statement block in " + parent.getID(), parent);
	}

	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}
	
	public void setIsUnderCondition(boolean cond) {
		this.scopeUnderCondition = cond;
	}
	
	public boolean isScopeUnderCondition() {
		return scopeUnderCondition;
	}
}
