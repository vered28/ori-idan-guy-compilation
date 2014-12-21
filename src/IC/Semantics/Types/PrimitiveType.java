package IC.Semantics.Types;

import IC.DataTypes;
import IC.Semantics.Scopes.ScopesVisitor;

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
	public PrimitiveType clone() {
		PrimitiveType newType = new PrimitiveType(type);
		newType.setDimension(getDimension());
		return newType;
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
		return super.equals(obj) &&  this.type.equals(type.getType());
	}

	@Override
	public boolean subTypeOf(Type otherType) {
		return otherType instanceof PrimitiveType &&
				((PrimitiveType)otherType).getType() == type;
	}

	public boolean isBoolean() {
		return type == DataTypes.BOOLEAN;
	}

	public boolean isInteger() {
		return type == DataTypes.INT;
	}

	public boolean isString() {
		return type == DataTypes.STRING;
	}

	public boolean isVoid() {
		return type == DataTypes.VOID;
	}

}
