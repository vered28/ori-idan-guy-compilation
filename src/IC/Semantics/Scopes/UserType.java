package IC.Semantics.Scopes;

import IC.AST.ICClass;

public class UserType extends Type {

	private String name;
	private ICClass usertype;
	
	public UserType(String name, ICClass usertype) {
		this.name = name;
		this.usertype = usertype;
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

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof UserType)) {
			return false;
		}

		UserType type = (UserType)obj;
		return super.equals(obj) && name.equals(type.getName());
	}

	@Override
	public boolean subTypeOf(Type otherType) {
		if (otherType instanceof UserType) {
			return usertype.subClassOf(((UserType)otherType).getClassNode());
		}
		return false;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	@Override
	public boolean isString() {
		return false;
	}

	@Override
	public boolean isVoid() {
		return false;
	}
}
