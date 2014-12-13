package IC.Semantics.Scopes;

import IC.AST.ICClass;

public class UserType extends Type {

	private String name;
	private ICClass usertype;
	
	public UserType(String name, ICClass usertype) {
		this.name = name;
		this.usertype = usertype;
	}
	
	public String getName() {
		return name;
	}
	
	public ICClass getClassNode() {
		return usertype;
	}

	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Object accept(ScopesVisitor visitor, IC.AST.Type type) {
		return visitor.visit(type);
	}
}
