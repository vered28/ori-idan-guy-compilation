package LIR.Translation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import IC.Semantics.Scopes.Symbol;
import LIR.Instructions.Register;

public class VariablesMap extends HashMap<Symbol, Register> {

	/* preserves all Map's behavior but only allows one Symbol
	 * to each Register (maintains a 1:1 relation, deletes all
	 * existing Symbol when new Symbol is mapped to register)
	 */
	
	@Override
	public Register put(Symbol symbol, Register reg) {
		
		List<Symbol> symbolsToRemove = new LinkedList<Symbol>();
		
		for (Symbol sym : keySet()) {
			if (get(sym).getNum() == reg.getNum()) {
				//of course, don't remove if it's simply the same symbol
				if (sym != symbol)
					symbolsToRemove.add(sym);
			}
		}
		
		for (Symbol sym : symbolsToRemove)
			remove(sym);
		
		return super.put(symbol, reg);
	}
	
	public Symbol getSymbol(Register reg) {
		for (Symbol sym : keySet()) {
			if (get(sym).getNum() == reg.getNum())
				return sym;
		}
		return null;
	}
	
}
