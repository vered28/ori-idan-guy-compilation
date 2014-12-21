package IC.Semantics;

import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Types.TypeTable;

public class ScopesTypesWrapper {

	private TypeTable typeTable;
	private ProgramScope globalScope;
	
	public ScopesTypesWrapper(TypeTable table, ProgramScope mainScope) {
		this.typeTable = table;
		this.globalScope = mainScope;
	}

	public TypeTable getTypeTable() {
		return this.typeTable;
	}

	public ProgramScope getGlobalScope() {
		return this.globalScope;
	}
	
}
