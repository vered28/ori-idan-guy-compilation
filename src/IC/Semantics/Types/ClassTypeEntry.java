package IC.Semantics.Types;


public class ClassTypeEntry extends TypeEntry {

	private UserType usertype;
	
	public ClassTypeEntry(String name, UserType usertype) {
		super(name, TypesEnum.ClassType);
		this.usertype = usertype;
	}
	
	public UserType getUserType() {
		return usertype;
	}

}
