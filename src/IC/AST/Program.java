package IC.AST;

import java.util.List;

/**
 * Root AST node for an IC program.
 * 
 * @author Tovi Almozlino
 */
public class Program extends ASTNode {

	private List<ICClass> classes;

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	/**
	 * Constructs a new program node.
	 * 
	 * @param classes
	 *            List of all classes declared in the program.
	 */
	public Program(List<ICClass> classes) {
		super(0, 0);
		this.classes = classes;
	}

	public List<ICClass> getClasses() {
		return classes;
	}
	
	/**
	 * Returns ICClass instance of ICClass with given parameter name.
	 * If no class by that name exists, returns null.
	 */
	public ICClass getClassByName(String name) {
		for (ICClass cls : classes) {
			if (cls.getName().equals(name))
				return cls;
		}
		return null;
	}

}
