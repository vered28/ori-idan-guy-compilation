package IC.AST;

/**
 * AST node for expression in parentheses.
 * 
 * @author Tovi Almozlino
 */
public class ExpressionBlock extends Expression {

	private Expression expression;

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	/**
	 * Constructs a new expression in parentheses node.
	 * 
	 * @param expression
	 *            The expression.
	 */
	public ExpressionBlock(Expression expression) {
		super(expression.getLine(), expression.getColumn());
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}

}
