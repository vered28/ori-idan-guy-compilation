package IC.Semantics.Types;

import IC.DataTypes;
import IC.AST.ICClass;
import IC.Semantics.Scopes.ScopesVisitor;

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
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public UserType clone() {
		UserType newType = new UserType(name, usertype);
		newType.setDimension(getDimension());
		return newType;
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
		
		//ignore arrays in names:
		
		String name1 = name;
		String name2 = type.getName();
		if (name1.indexOf('[') > 0)
			name1 = name1.substring(0, name1.indexOf('['));
		if (name2.indexOf('[') > 0)
			name2 = name2.substring(0, name2.indexOf('['));

		return super.equals(obj) && name1.equals(name2);
	}

	@Override
	public boolean subTypeOf(Type otherType) {
		if (otherType instanceof PrimitiveType) {
			if (((PrimitiveType)otherType).getType() == DataTypes.NULL) {
				//everything is derived from null
				return true;
			}
		} else if (otherType instanceof UserType) {
			return usertype.subClassOf(((UserType)otherType).getClassNode());
		}
		return false;
	}
	
}
