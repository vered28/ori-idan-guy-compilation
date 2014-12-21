package IC.Semantics.Types;

import IC.AST.Formal;
import IC.AST.Method;
import IC.Semantics.Scopes.ScopesVisitor;

public class MethodType extends Type {

	private Method method;
	private String name;
	
	public MethodType(Method method) {
		this.method = method;
		this.name = buildName();
		
	}
	
	private String buildName() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		
		boolean first = true;
		
		for (Formal formal : method.getFormals()) {

			if (first) { first = false; }
			else       { sb.append(", "); }
			
			sb.append(formal.getType().getName());
			
			for (int i = 0; i < formal.getType().getDimension(); i++) {
				sb.append("[]");
			}
		}
		
		sb.append(" -> ");
		sb.append(method.getType().getName());
		for (int i = 0; i < method.getType().getDimension(); i++) {
			sb.append("[]");
		}
		
		sb.append("}");
		
		return sb.toString();
	}
	
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Object accept(ScopesVisitor visitor, IC.AST.Type type) {
		return visitor.visit(type);
	}

	public Method getMethod() {
		return method;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean subTypeOf(Type otherType) {
		//methods are not subtypes (even when they override)
		return false;
	}
	
	@Override
	public void setDimension(int dimension) {
		super.setDimension(0); //methods can have dimensions
	}

	@Override
	public Type clone() {
		//methods don't need to be cloned
		return null;
	}

}
