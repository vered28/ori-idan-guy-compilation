package IC.Semantics.Scopes;

import java.util.HashMap;
import java.util.Map;

public class Scope {

	private Map<String, Symbol> symbols;
	private String id;
	
	private Scope parentScope;
	
	public Scope(String id) {
		this(id, null);
	}
	
	public Scope(String id, Scope parent) {
		this.id = id;
		this.parentScope = parent;
		this.symbols = new HashMap<String, Symbol>();
	}
	
	public Scope getParentScope() {
		return parentScope;
	}
	
	public void setParentScope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	public String getID() {
		return id;
	}
	
	public void addToScope(Symbol symbol) throws Exception {
		
		if (symbols.containsKey(symbol.getID())) {
			//throw checked exception to force catching and handling:
			throw new Exception("symbol " + symbol.getID() + " defined more than once in scope.");
		}
		
		symbols.put(symbol.getID(), symbol);
		
	}
	
	public Symbol getSymbol(String id) {
		return symbols.get(id);
	}
	
	public boolean containsSymbol(String id) {
		return (getSymbol(id) != null);
	}
	
}
