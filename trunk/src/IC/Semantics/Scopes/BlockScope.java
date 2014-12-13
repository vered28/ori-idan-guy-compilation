package IC.Semantics.Scopes;

public class BlockScope extends Scope {

	private static int blockNum = 0;
	
	public BlockScope() {
		this(null);
	}
	
	public BlockScope(Scope parent) {
		super("block " + ++blockNum, parent);
	}

}
