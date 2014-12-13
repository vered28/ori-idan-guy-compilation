package IC.Semantics.Scopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ScopeMap extends HashMap<String, Symbol> {

	private static final long serialVersionUID = 1L;

	private Comparator<String> comparator;
	
	public ScopeMap() {
		this.comparator = defaultComparator();
	}
	
	public ScopeMap(Comparator<String> comparator) {
		this.comparator = comparator;
	}
	
	public List<Symbol> getSymbols() {
		
		//get all keys from map as array of strings:
		String[] keys = super.keySet().toArray(new String[0]);
		
		//sort using comparator:
		Arrays.sort(keys, comparator);
		
		//build list in sorted order of symbols:
		ArrayList<Symbol> symbols = new ArrayList<Symbol>(keys.length);
		for (String id : keys) {
			symbols.add(get(id));
		}
		
		return symbols;
		
	}
	
	public Comparator<String> getComparator() {
		return comparator;
	}
	
	public void setComparator(Comparator<String> comparator) {
		this.comparator = comparator;
	}
	
	private Comparator<String> defaultComparator() {
		return new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int line1 = get(o1).getNode().getLine();
				int line2 = get(o2).getNode().getLine();
				int column1 = get(o1).getNode().getColumn();
				int column2 = get(o2).getNode().getColumn();
				
				if (line1 == line2)
					return column1 - column2;
				return line1 - line2;
			}
			
		};
	}
}
