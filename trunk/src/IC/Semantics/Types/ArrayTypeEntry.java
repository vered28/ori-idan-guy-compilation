package IC.Semantics.Types;

public class ArrayTypeEntry extends TypeEntry {
	
	//for A[], A[][] and so on, type entry will be A's type entry.
	private TypeEntry typeEntry;

	public ArrayTypeEntry(String name, TypeEntry typeEntry) {
		super(name, TypesEnum.ArrayType);
	}
	
	public TypeEntry getTypeEntry() {
		return typeEntry;
	}

}
