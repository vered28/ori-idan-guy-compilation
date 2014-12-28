package LIR.Instructions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import IC.AST.ASTNode;
import IC.AST.ICClass;
import LIR.Translation.DispatchTable;
import LIR.Translation.StringLiteralSet;

public class LIRProgram extends LIRInstruction {

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;

	private List<LIRClass> classes;
	
	public LIRProgram(ASTNode node, StringLiteralSet literals,
			Map<ICClass, DispatchTable> dispatchTables) {
		super(node);
		this.classes = new LinkedList<LIRClass>();
		this.literals = literals;
		this.dispatchTables = dispatchTables;
	}

	@Override
	public Object accept(LIRVisitor visitor) {
		return visitor.visit(this);
	}
	
	public void addClass(LIRClass lirclass) {
		classes.add(lirclass);
	}
	
	public List<LIRClass> getClasses() {
		return classes;
	}

	public StringLiteralSet getLiterals() {
		return this.literals;
	}

	public Map<ICClass, DispatchTable> getDispatchTables() {
		return this.dispatchTables;
	}

}
