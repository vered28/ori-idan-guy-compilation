package IC.Semantics.Scopes;

import IC.AST.ASTNode;
import IC.Semantics.Types.Type;

public class Symbol {

	private String id;
	private Type type;
	private Kind kind;
	private ASTNode node;
	
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
	
}
