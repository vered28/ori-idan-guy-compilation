package IC.Semantics.Scopes;

public enum Kind {

	CLASS(),
	VAR(), //variable (including fields for Program, formals for Method and so on)
	METHOD();
	
	private Kind() { }
	
}
