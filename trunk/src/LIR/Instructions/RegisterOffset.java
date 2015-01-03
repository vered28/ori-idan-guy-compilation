package LIR.Instructions;

import IC.AST.ASTNode;
import IC.Semantics.Scopes.Symbol;

public class RegisterOffset extends Operand {

	private final Register register;
	private final BasicOperand offset;
	
	private Symbol symbol;
	
	public RegisterOffset(ASTNode node, Register register, BasicOperand offset) {
		this(node, register, offset, null);
	}
	
	public RegisterOffset(ASTNode node, Register register, BasicOperand offset, Symbol symbol) {
		super(node);
		this.register = register;
		this.offset = offset;
		this.symbol = symbol;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public Register getRegister() {
		return register;
	}

	public BasicOperand getOffset() {
		return offset;
	}

	public Symbol getSymbol() {
		return this.symbol;
	}

	public void setSymbol(Symbol symbol) {
		this.symbol = symbol;
	}
	
}
