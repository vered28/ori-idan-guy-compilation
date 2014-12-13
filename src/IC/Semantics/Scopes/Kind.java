package IC.Semantics.Scopes;

public enum Kind {

	CLASS("Class"),
	FIELD("Field"), //variable (including fields for Program)
	FORMAL("Parameter"),
	STATICMETHOD("Static method"),
	VIRTUALMETHOD("Virtual method"),
	VARIABLE("Local variable");
	
	private String value;
	
	private Kind(String value) { this.value = value; }
	
	public String getValue() {
		return value;
	}
	
}
