package IC.Semantics.Types;

public enum TypesEnum {

	PrimitiveType(1, "Primitive type"),
	ClassType(2, "Class"),
	ArrayType(3, "Array type"),
	MethodType(4, "Method type");
	
	private int precedence;
	private String description;
	
	private TypesEnum(int precedence, String description) {
		this.precedence = precedence;
		this.description = description;
	}
	
	public int getPrecedence() {
		return precedence;
	}
	
	public String getDescription() {
		return description;
	}
	
	public TypesEnum fromAST(IC.AST.Type type) {
		
		if (type.getDimension() > 0)
			return TypesEnum.ArrayType;
		
		if (type instanceof IC.AST.PrimitiveType)
			return TypesEnum.PrimitiveType;
		
		if (type instanceof IC.AST.UserType)
			return TypesEnum.ClassType;
		
		return TypesEnum.MethodType;
		
	}
}
