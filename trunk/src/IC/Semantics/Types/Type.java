package IC.Semantics.Types;

import IC.Semantics.Scopes.ScopesVisitor;

public abstract class Type {

	//not much different than AST's Type nodes. Created here for
	//better separation and abstraction between sub-packages.

	//one of the visitors' accept() links between AST type and SymbolTable type.
	
	public abstract Object accept(ScopesVisitor visitor);

	public abstract Object accept(ScopesVisitor visitor, IC.AST.Type type);
	
	//we use clone() because type table holds a single copy of
	//each type. however, during checks and traversing, we may
	//request an int from the table only to change its' dimension
	//during traversal. We don't want this to affect other types,
	//statements, expressions and so on.
	@Override
	public abstract Type clone();

	public abstract String getName();

	public abstract boolean subTypeOf(Type otherType);

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

	@Override
	public boolean equals(Object obj) {
		return getDimension() == ((Type)obj).getDimension();
	}
	
	//optional for override
	public void setName(String name) { }

}
