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

	@Override
	public String getName() {
		return type.getDescription();
	}

	@Override
	public String toString() {
		return type.getDescription();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrimitiveType)) {
			return false;
		}

		PrimitiveType type = (PrimitiveType)obj;
		return super.equals(obj) &&  this.type.equals(type);
	}

	@Override
	public boolean subTypeOf(Type otherType) {
		return otherType instanceof PrimitiveType &&
				((PrimitiveType)otherType).getType() == type;
	}

	@Override
	public boolean isBoolean() {
		return type == DataTypes.BOOLEAN;
	}

	@Override
	public boolean isInteger() {
		return type == DataTypes.INT;
	}

	@Override
	public boolean isString() {
		return type == DataTypes.STRING;
	}

	@Override
	public boolean isVoid() {
		return type == DataTypes.VOID;
	}

}
