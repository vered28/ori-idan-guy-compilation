package IC.Semantics.Validations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import IC.AST.ICClass;
import IC.AST.Type;
import IC.Semantics.Exceptions.SemanticError;
import IC.Semantics.Scopes.BlockScope;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.MethodScope;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Scopes.Scope;
import IC.Semantics.Scopes.ScopesVisitor;
import IC.Semantics.Scopes.Symbol;
import IC.Semantics.Types.MethodType;
import IC.Semantics.Types.PrimitiveType;
import IC.Semantics.Types.UserType;

public class NonCircularScopesValidation implements ScopesVisitor {

	/* This class checks that the scopes graph we built in ScopesBuilder
	 * is in fact a tree: There is no circle among any nodes.
	 * 
	 * How may we get a circle? Only by inheritance.
	 * For example: A extends B, B extends C, C extends A.
	 * 
	 */
	
	private Map<String, Integer> notDirectChildClassesVisits;
	private int maxVisitsAllowed;
	private ICClass currentICClass; //link to node to help with error messaging
	
	@Override
	public Object visit(ProgramScope program) {

		List<Symbol> symbols = program.getSymbols();
		List<Scope>  children = program.getChildrenScopes();
		
		//if symbols and children have the same size, there is
		//no inheritance and therefore scopes graph isn't circular
		if (symbols.size() != children.size()) {
			
			/* each class symbol in global symbol table (ProgramScope)
			 * that is not a child of the table, is a child of
			 * another scope, a ClassScope, and is therefore inheriting.
			 * visit each one of those class scopes and make sure
			 * you see each one only once (since visit is top-down). */
			
			//remove all children from symbols, so symbols will only
			//be those who are not a direct child of ProgramScope:
			for (Scope child : children) {
				symbols.remove(program.getSymbol(child.getID()));
			}
			
			notDirectChildClassesVisits = new HashMap<String, Integer>();
			
			//each class can only be visited as many times as there are
			//inherited classes (for worst case scenario of list of inheritance
			//					 A->B->C->D->...->Z)
			maxVisitsAllowed = symbols.size();
			
			for (Symbol symbol : symbols) {
				currentICClass = (ICClass)symbol.getNode();
				symbol.getNode().getEnclosingScope().accept(this);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(ClassScope icClass) {
		
		//how many times have we visited this class already?
		int visited = 1;
		if (!notDirectChildClassesVisits.containsKey(icClass.getID()))
			notDirectChildClassesVisits.put(icClass.getID(), 1);
		else
			visited = notDirectChildClassesVisits.get(icClass.getID());
		
		if (visited > maxVisitsAllowed) {
			//we've visited this class before --> circular scopes:
			throw new SemanticError("Class hierarchy is circular, must be a tree!",
					currentICClass.getLine(),
					currentICClass.getColumn());
		}
	
		//update count in map:
		notDirectChildClassesVisits.put(icClass.getID(), visited+1);
		
		//visit all child scopes.
		//If all children are not classes, we're done.
		//If any children are classes, then as long as none of them have
		//this class instance as a child, we won't visit this class again
		//under this branch (might visit from another program scope call)
		for (Scope child : icClass.getChildrenScopes()) {
			child.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(BlockScope block) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MethodScope method) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(PrimitiveType type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(UserType type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Type type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MethodType type) {
		//do nothing
		return null;
	}

}
