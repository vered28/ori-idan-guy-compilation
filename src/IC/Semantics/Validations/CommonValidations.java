package IC.Semantics.Validations;

import IC.Semantics.Exceptions.StaticVirtualAmbiguityException;
import IC.Semantics.Scopes.BlockScope;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.MethodScope;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.Symbol;

public class CommonValidations {

	public static Scope getClassScopeByName(Scope currentScope, String className) {
		
		//run current until root (ProgramScope):
		while (currentScope.getParentScope() != null) {
			currentScope = currentScope.getParentScope();
		}
		
		//run over all symbols (all program classes) and return
		//scope of class with given ID className:
		for (Symbol symbol : currentScope.getSymbols()) {
			if (symbol.getKind() == Kind.CLASS) { //all should be classes, but make sure anyway
				if (symbol.getID().equals(className))
					return symbol.getNode().getEnclosingScope();
			}
		}
		
		//no class found by that name (Symbol ID), return null:
		return null;
		
	}
	
	public static Scope getClassScopeOfCurrentScope(Scope currentScope) {
		
		if (currentScope == null)
			return null;
		
		if (currentScope instanceof MethodScope
				&& !(currentScope instanceof BlockScope))
			return currentScope.getParentScope(); //parent of method is always class
		
		return getClassScopeOfCurrentScope(currentScope.getParentScope());
	}
	
	public static Symbol findSymbol(String id, Kind kind, Scope scope) {
		return findSymbol(id, kind, scope, false);
	}
	
	public static Symbol findSymbol(String id, Kind kind, Scope scope, boolean onlyCheckInMethodScope) {
		
		if (scope == null || (onlyCheckInMethodScope && !(scope instanceof MethodScope)))
			return null;
		
		try {
			if (scope.containsSymbol(id)) {
				if (scope.getSymbol(id).getKind() == kind)
					return scope.getSymbol(id);
			}
		} catch (StaticVirtualAmbiguityException e) {
			if (kind.equals(Kind.STATICMETHOD)) {
				Symbol sym = ((ClassScope)scope).getStaticSymbol(id);
				if (sym != null)
					return sym;
			} else if (kind.equals(Kind.VIRTUALMETHOD)) {
				Symbol sym = ((ClassScope)scope).getSymbol(id, true);
				if (sym != null)
					return sym;				
			}
		}
		
		return findSymbol(id, kind, scope.getParentScope(), onlyCheckInMethodScope);
		
	}
	
	public static Symbol findSymbol(String id, Kind kind, Scope scope, int aboveLine) {
		return findSymbol(id, kind, scope, aboveLine, false);
	}
	
	/**
	 * finds symbol with id of kind in scope hierarchy starting with scope.
	 * 
	 * @param aboveLine for kind.Variable searches in the first scope only for
	 * symbols above the stated line. This helps avoid circumstances where symbol
	 * from another scope was referenced, then a symbol with the same name was
	 * declared (and say a different type).
	 * 
	 * @param onlyCheckInMethodScope used for static methods. Checks for variables /
	 * formals existence only in method scope (everything outside the method is
	 * part of the instance scope - a virtual scope).
	 */
	public static Symbol findSymbol(String id, Kind kind, Scope scope, int aboveLine, boolean onlyCheckInMethodScope) {
	
		if (!kind.equals(Kind.VARIABLE))
			return findSymbol(id, kind, scope, onlyCheckInMethodScope);
		
		if (scope.containsSymbol(id)) {
			Symbol symbol = scope.getSymbol(id);
			if (symbol.getKind() == kind) {
				if (symbol.getNode().getLine() <= aboveLine) {
					//symbol was found in a previous line (lower
					//line number).
					return scope.getSymbol(id);
				}
			}
		}
		
		return findSymbol(id, kind, scope.getParentScope(), onlyCheckInMethodScope);

	}
}