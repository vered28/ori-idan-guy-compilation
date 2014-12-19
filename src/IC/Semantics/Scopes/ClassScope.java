package IC.Semantics.Scopes;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IC.AST.ASTNode;
import IC.Semantics.StaticVirtualAmbiguityException;

public class ClassScope extends Scope {
	
	private Map<String, Symbol> staticSymbols;

	public ClassScope(String id) {
		this(id, null, null);
	}
	
	public ClassScope(String id, Scope parent) {
		this(id, parent, null);
	}
	
	public ClassScope(String id, ASTNode node) {
		this(id, null, node);
	}
	
	public ClassScope(String id, Scope parent, ASTNode node) {
		super(id, parent);
		staticSymbols = new ScopeMap();
		setComparator();
	}
	
	private void setComparator() {

		setComparator(new Comparator<String>() {
			
			//for methods with the same name (string id) who have been declared
			//twice (once as static, once as virtual) store a counter and handle
			//accordingly:
			private Map<String, Boolean> declaredTwice = new HashMap<String, Boolean>();
			
			@Override
			public int compare(String o1, String o2) {				
				
				Symbol s1 = null, s2 = null;
				Symbol[] s = { s1, s2 };
				String[] o = { o1, o2 };
				
				for (int i = 0; i < 2; i++) {
					try {
						s[i] = getSymbol(o[i]);
					} catch (StaticVirtualAmbiguityException e) {
						if (declaredTwice.containsKey(o[i])) {
							//second time we've seen it, handle the virtual one:
							s[i] = getSymbol(o[i], true);
							//same symbol was compared twice, remove from map so
							//compare will start from static again on next comparison:
							declaredTwice.remove(o[i]);
						} else {
							//handle the static one first:
							s[i] = getStaticSymbol(o[i]);
							declaredTwice.put(o[i], true);
						}
					}					
				}
				
				s1 = s[0];
				s2 = s[1];

				int line1 = s1.getNode().getLine();
				int line2 = s2.getNode().getLine();
				int column1 = s1.getNode().getColumn();
				int column2 = s2.getNode().getColumn();
				
				if (line1 == line2)
					return column1 - column2;
				return line1 - line2;
			}
		});
		
	}
	
	@Override
	public void addToScope(Symbol symbol) throws Exception {
		
		if (symbol.getKind().equals(Kind.STATICMETHOD)) {
			if (staticSymbols.containsKey(symbol.getID())) {
				//throw checked exception to force catching and handling:
				throw new Exception(symbol.getKind().getValue() + " " + symbol.getID() + " defined more than once in scope " + getID() + ".");
			}
			staticSymbols.put(symbol.getID(), symbol);
		} else {		
			super.addToScope(symbol);
		}
	}

	@Override
	public List<Symbol> getSymbols() {
		List<Symbol> symbols = super.getSymbols();
		symbols.addAll(((ScopeMap)staticSymbols).getSymbols());
		Collections.sort(symbols, new Comparator<Symbol>() {

			@Override
			public int compare(Symbol o1, Symbol o2) {
				return getComparator().compare(o1.getID(), o2.getID());
			}
			
		});
		return symbols;
	}
	
	@Override
	public Symbol getSymbol(String id) {
		
		Symbol parentSymbol = super.getSymbol(id);
		Symbol staticSymbol = staticSymbols.get(id);
		
		if (staticSymbol == null) {
			return parentSymbol;
		} else {
			if (parentSymbol == null) {
				return staticSymbol;
			}
			throw new StaticVirtualAmbiguityException("Symbol " + id + " is defined twice in the same scope: " + getID());
		}
		
	}
	
	/**
	 * Returns Symbol from parent scope (ignoring all symbols with same id defined
	 * in current scope). This is used to retrieve non-static symbols.
	 *  
	 * @param circumventToParent When true, takes the symbol from the parent scope
	 */
	public Symbol getSymbol(String id, boolean circumventToParent) {
		if (circumventToParent)
			return super.getSymbol(id);
		return this.getSymbol(id);
	}
	
	/**
	 * Returns the symbol from the static scope of the class symbol table
	 */
	public Symbol getStaticSymbol(String id) {
		return staticSymbols.get(id);
	}
		
	@Override
	public Object accept(ScopesVisitor visitor) {
		return visitor.visit(this);
	}

}
