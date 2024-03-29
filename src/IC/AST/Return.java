package IC.AST;

/**
 * Return statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class Return extends Statement {

	private Expression value = null;

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	/**
	 * Constructs a new return statement node, with no return value.
	 * 
	 * @param line
	 *            Line number of return statement.
	 */
	public Return(int line, int column) {
		super(line, column);
	}

	/**
	 * Constructs a new return statement node.
	 * 
	 * @param line
	 *            Line number of return statement.
	 * @param value
	 *            Return value.
	 */
	public Return(int line, int column, Expression value) {
		this(line, column);
		this.value = value;
	}

	public boolean hasValue() {
		return (value != null);
	}

	public Expression getValue() {
		return value;
	}

}
