package IC.Semantics.Scopes;

public class Symbol {

	private String id;
	private Type type;
	private Kind kind;
	private boolean isStatic;
	
	public Symbol(String id, Type type, Kind kind) {
		this(id, type, kind, false);
	}

	public Symbol(String id, Type type, Kind kind, boolean isStatic) {
		this.id = id;
		this.type = type;
		this.kind = kind;
		this.isStatic = isStatic;
	}

	public String getID() {
		return this.id;
	}

	public Type getType() {
		return this.type;
	}

	public Kind getKind() {
		return this.kind;
	}

	public boolean isStatic() {
		return this.isStatic;
	}
	
}
