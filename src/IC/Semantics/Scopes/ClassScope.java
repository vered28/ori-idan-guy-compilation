package IC.Semantics.Scopes;

public class ClassScope extends Scope {

	public ClassScope(String id) {
		this(id, null);
	}

	public ClassScope(String id, Scope parent) {
		super(id, parent);
		
		//add this to scope and ignore exception (cannot be thrown, nothing
		//has been added yet):
		try {
			addToScope(new Symbol("this", Type.THIS, Kind.CLASS, false));
		} catch (Exception e) {
			//ignore / do nothing
		}
	}

}
