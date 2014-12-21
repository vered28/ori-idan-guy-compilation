package IC.Semantics.Types;

public abstract class TypeEntry {

	private String name;
	private TypesEnum type;
	private int serialID;
	
	private static int counter = 0;
	
	public TypeEntry(String name, TypesEnum type) {
		this.name = name;
		this.type = type;
		this.serialID = ++counter;
	}

	public String getName() {
		return name;
	}
		
	public TypesEnum getType() {
		return type;
	}

	public int getSerialID() {
		return serialID;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj == this)
			return true;
		
		if (!(obj instanceof TypeEntry))
			return false;
		
		//only one type with each name can exist!
		return ((TypeEntry)obj).getName().equals(name);
	}
	
}
