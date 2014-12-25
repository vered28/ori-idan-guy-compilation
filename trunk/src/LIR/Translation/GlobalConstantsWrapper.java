package LIR.Translation;

import java.util.Map;

import IC.AST.ICClass;

public class GlobalConstantsWrapper {

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;
	
	public GlobalConstantsWrapper(
			StringLiteralSet literals, Map<ICClass, DispatchTable> dispatchTables) {
		this.literals = literals;
		this.dispatchTables = dispatchTables;
	}

	public StringLiteralSet getLiterals() {
		return this.literals;
	}

	public Map<ICClass, DispatchTable> getDispatchTables() {
		return this.dispatchTables;
	}
	
}
