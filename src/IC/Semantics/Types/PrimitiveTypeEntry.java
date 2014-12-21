package IC.Semantics.Types;

import IC.DataTypes;

public class PrimitiveTypeEntry extends TypeEntry {

	private PrimitiveType type;
	
	public PrimitiveTypeEntry(String name, DataTypes datatype) {
		super(name, TypesEnum.PrimitiveType);
		this.type = new PrimitiveType(datatype);
	}
	
	public PrimitiveType getPrimitiveType() {
		return type;
	}

}
