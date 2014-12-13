package IC.Semantics.Scopes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class Scope {

	private Map<String, Symbol> symbols;
	private String id;
	
	private Scope parentScope;
	private List<Scope> childrenScopes;
	
	public Scope(String id) {
		this(id, null);
	}
	
	public Scope(String id, Scope parent) {
		this.id = id;
		this.parentScope = parent;
		this.symbols = new ScopeMap();
		this.childrenScopes = new ArrayList<Scope>();
	}
	
	public abstract Object accept(ScopesVisitor visitor);
	
	public void setComparator(Comparator<String> comparator) {
		((ScopeMap)symbols).setComparator(comparator);
	}
	
	public Comparator<String> getComparator() {
		return ((ScopeMap)symbols).getComparator();
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
	
	public List<Symbol> getSymbols() {
		return ((ScopeMap)symbols).getSymbols();
	}
	
	public boolean containsSymbol(String id) {
		return (getSymbol(id) != null);
	}
		
	public void addChildScope(Scope child) {
		childrenScopes.add(child);
	}
	
	/**
	 * removes child parameter from children list, if exists
	 * (does nothing otherwise)
	 */
	public void removeChildScope(Scope child) {
		childrenScopes.remove(child);
	}
	
	public List<Scope> getChildrenScopes() {
		return childrenScopes;
	}
	
	/**
	 * returns child scope with the given id, or null if there is
	 * no child with this id
	 */
	public Scope getChildScope(String id) {
		for (Scope scope : childrenScopes) {
			if (scope.getID().equals(id))
				return scope;
		}
		return null;
	}
	
}
