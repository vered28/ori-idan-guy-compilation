package IC.AST;

/**
 * Abstract base class for expression AST nodes.
 * 
 * @author Tovi Almozlino
 */
public abstract class Expression extends ASTNode {

	private int weight = -1;
	
	/**
	 * Constructs a new expression node. Used by subclasses.
	 * 
	 * @param line
	 *            Line number of expression.
	 */
	protected Expression(int line, int column) {
		super(line, column);
	}
	
	public int getWeight() {
		return this.weight;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
}