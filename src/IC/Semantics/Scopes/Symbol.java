package IC.Semantics.Scopes;

import IC.AST.ASTNode;
import IC.Semantics.Types.Type;

public class Symbol {

	private String id;
	private Type type;
	private Kind kind;
	private ASTNode node;
	
	private boolean hasValue = false;
	private Object value = null;
	
	public Symbol(String id, Type type, Kind kind, ASTNode node) {
		this.id = id;
		this.type = type;
		this.kind = kind;
		this.node = node;
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
	
	public ASTNode getNode() {
		return node;
	}

	public boolean hasValue() {
		return this.hasValue;
	}

	public Object getValue() {
		return this.value;
	}

	public void setHasValue(boolean hasValue) {
		this.hasValue = hasValue;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
}
