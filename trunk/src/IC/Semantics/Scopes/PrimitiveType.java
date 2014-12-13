package IC.Semantics.Scopes;

import IC.DataTypes;

public class PrimitiveType extends Type {

	private DataTypes type;
	
	public PrimitiveType(DataTypes type) {
		this.type = type;
	}
	
	public DataTypes getType() {
		return type;
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public Object accept(ScopesVisitor visitor, IC.AST.Type type) {
		return visitor.visit(type);
	}
	
}
