package IC.Semantics.Scopes;


public enum Type {

	//not much different than AST's Type nodes. Created as enum here
	//for better separation and abstraction between sub-packages.
	
	INT("int"),
	BOOLEAN("boolean"),
	STRING("string"),
	VOID("void"),
	USERTYPE("Class"), //user-defined class
	THIS("this");
	
	//represents [] for arrays (i.e dimension = 2 is for Type[][]).
	//non-array types have default dimension 0.
	private int dimension = 0;
	
	private String value;
	
	private Type(String value) { this.value = value; } ;
	
	public void setDimension(int dimension) {
		if (dimension < 0)
			throw new IllegalArgumentException("Type dimension must be non-negative.");
		this.dimension = dimension;
	}
	
	public int getDimension() {
		return dimension;
	}
	
	public String getValue() {
		return value;
	}
	
}
