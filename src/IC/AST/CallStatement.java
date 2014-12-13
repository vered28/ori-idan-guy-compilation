package IC.AST;

/**
 * Method call statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class CallStatement extends Statement {

	private Call call;

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	/**
	 * Constructs a new method call statement node.
	 * 
	 * @param call
	 *            Method call expression.
	 */
	public CallStatement(Call call) {
		super(call.getLine(), call.getColumn());
		this.call = call;
	}

	public Call getCall() {
		return call;
	}

}
