package LIR.Translation;

import java.util.Collection;
import java.util.HashSet;

import IC.AST.Literal;

public class StringLiteralSet extends HashSet<StringLiteral> {

	/* extends HashSet in order to improve string search (to prevent
	 * duplicate literals and unnecessary growth of code). On average,
	 * HashSet provides O(1) time for searching and inserting.
	 * Since we built the set in one sequential run of the AST tree,
	 * we can use average and amortized analysis to assume better
	 * running time. */
		
	public boolean add(Literal literal) {
		
		if (contains(literal.getValue() + ""))
			return false;
		
		return add(new StringLiteral(literal));
		
	}
	
	@Override
	public boolean add(StringLiteral e) {
		
//		if (contains(e))
//			return false;
		
		return super.add(e);
	}
	
	@Override
	public boolean addAll(Collection<? extends StringLiteral> c) {
		boolean changed = false;
		for (StringLiteral literal : c) {
			changed |= add(literal);
		}
		return changed;
	}
	
	public StringLiteral get(String str) {
		for (StringLiteral sl : this) {
			if (sl.getLiteral().getValue().equals(str))
				return sl;
		}
		return null;
	}
	
}
