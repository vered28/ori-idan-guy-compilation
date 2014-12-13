package IC.Semantics.Scopes;


public abstract class Type {

	//not much different than AST's Type nodes. Created as enum here
	//for better separation and abstraction between sub-packages.
		
	//one of the visitors' accept() links between AST type and SymbolTable type.
	
	public abstract Object accept(ScopesVisitor visitor);

	public abstract Object accept(ScopesVisitor visitor, IC.AST.Type type);
	
	//represents [] for arrays (i.e dimension = 2 is for Type[][]).
	//non-array types have default dimension 0.
	private int dimension = 0;
	
	public void setDimension(int dimension) {
		if (dimension < 0)
			throw new IllegalArgumentException("Type dimension must be non-negative.");
		this.dimension = dimension;
	}
	
	public int getDimension() {
		return dimension;
	}
	
}
