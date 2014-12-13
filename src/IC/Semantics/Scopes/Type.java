package IC.Semantics.Scopes;

import IC.AST.PrimitiveType;

public enum Type {

	//not much different than AST's Type nodes. Created as enum here
	//for better separation and abstraction between sub-packages.
	
	INT(),
	BOOLEAN(),
	STRING(),
	VOID(),
	USERTYPE(), //user-defined class
	THIS();
	
	//represents [] for arrays (i.e dimension = 2 is for Type[][]).
	//non-array types have default dimension 0.
	private int dimension = 0;
	
	private Type() { } ;
	
	public void setDimension(int dimension) {
		if (dimension < 0)
			throw new IllegalArgumentException("Type dimension must be non-negative.");
		this.dimension = dimension;
	}
	
	public int getDimension() {
		return dimension;
	}
	
	public static Type mapFromAstType(IC.AST.Type type) {
		
		if (type instanceof PrimitiveType) {
			String astName = ((PrimitiveType)type).getName();
			if (astName.equals("int"))
				return Type.INT;
			if (astName.equals("void"))
				return Type.VOID;
			if (astName.equals("boolean"))
				return Type.BOOLEAN;
			if (astName.equals("string"))
				return Type.STRING;
		} else {
			return Type.USERTYPE;
		}
		
		return null; //this is bad, should never get here!
		
	}
	
}
