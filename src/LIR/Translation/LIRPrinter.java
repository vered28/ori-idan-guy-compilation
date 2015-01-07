package LIR.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import IC.AST.Field;
import IC.AST.ICClass;
import IC.AST.Method;
import IC.Semantics.Scopes.ClassScope;
import IC.Semantics.Scopes.Kind;
import IC.Semantics.Scopes.ScopesTraversal;
import IC.Semantics.Scopes.Symbol;
import LIR.Instructions.ArrayLength;
import LIR.Instructions.ArrayLoad;
import LIR.Instructions.ArrayLocation;
import LIR.Instructions.ArrayStore;
import LIR.Instructions.BasicOperand;
import LIR.Instructions.BinaryArithmetic;
import LIR.Instructions.BinaryLogical;
import LIR.Instructions.ConstantDispatch;
import LIR.Instructions.ConstantInteger;
import LIR.Instructions.ConstantNull;
import LIR.Instructions.ConstantString;
import LIR.Instructions.FieldLoad;
import LIR.Instructions.FieldStore;
import LIR.Instructions.Jump;
import LIR.Instructions.LIRClass;
import LIR.Instructions.LIRInstruction;
import LIR.Instructions.LIRMethod;
import LIR.Instructions.LIRProgram;
import LIR.Instructions.LIRVisitor;
import LIR.Instructions.Label;
import LIR.Instructions.LibraryCall;
import LIR.Instructions.Memory;
import LIR.Instructions.Move;
import LIR.Instructions.Register;
import LIR.Instructions.RegisterOffset;
import LIR.Instructions.Return;
import LIR.Instructions.StaticCall;
import LIR.Instructions.UnaryArithmetic;
import LIR.Instructions.UnaryLogical;
import LIR.Instructions.VirtualCall;

public class LIRPrinter implements LIRVisitor {

	private LIRMethod mainMethod = null;
	private Map<ICClass, DispatchTable> dispatchTables = null;
	
	@Override
	public Object visit(LIRProgram program) {

		StringBuilder sb = new StringBuilder();
		
		//first print string literals:
		
		List<StringLiteral> sortedLiterals =
				new ArrayList<StringLiteral>(program.getLiterals());
		Collections.sort(sortedLiterals, new Comparator<StringLiteral>() {

			@Override
			public int compare(StringLiteral o1, StringLiteral o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		
		for (StringLiteral literal : sortedLiterals) {
			sb.append(literal.getId());
			sb.append(": ");
			sb.append(literal.getLiteral().getType().toFormattedString(
					literal.getLiteral().getValue()));
			sb.append("\n");
		}
				
		//next, print dispatch tables:
		dispatchTables = program.getDispatchTables();
		for (ICClass icClass : program.getDispatchTables().keySet()) {
			
			DispatchTable table = program.getDispatchTables().get(icClass);
			
			if (table.getMethods().size() == 0)
				continue;
			
			sb.append(table.getName());
			sb.append(": [");
			
			boolean first = true;
			for (Method method : table.getMethods()) {
				
				if (first)
					first = false;
				else
					sb.append(", ");
				
				ICClass methodOfClass = icClass;
				if (icClass.hasSuperClass()) {
					//might belong to parent:
					Symbol symbol = ScopesTraversal.findSymbol(method.getName(),
							Kind.VIRTUALMETHOD, icClass.getEnclosingScope());
					if (symbol != null) {
						methodOfClass = ScopesTraversal.getICClassFromClassScope(
								(ClassScope)ScopesTraversal.getClassScopeByName(
										icClass.getEnclosingScope(),
										symbol.getNode().getEnclosingScope().getParentScope().getID())
										);
					}
				}
				
				sb.append(LabelMaker.methodString(methodOfClass, method));
				
			}
			
			sb.append("]");
			sb.append("\n");
			
		}
		
		sb.append("\n");
		
		for (LIRClass lirclass : program.getClasses()) {
			sb.append(lirclass.accept(this));
		}

		//print main method last:
		if (mainMethod != null)
			sb.append(mainMethod.accept(this));

		return sb.toString().trim(); //trim multiple new lines if exists
	}

	@Override
	public Object visit(LIRClass lirclass) {

		StringBuilder sb = new StringBuilder();
		
		ICClass cls = (ICClass)lirclass.getAssociactedICNode();
		DispatchTable dt = dispatchTables.get(cls);
		
		if (dt.getFields().size() > 0) {
			
			sb.append("# ");
			sb.append(cls.getName());
			sb.append(" class fields offset:");
			sb.append("\n");
			sb.append("#---------------------------------------------");
			sb.append("\n");
			
			
			for (Field f : dt.getFields()) {
				sb.append("# ");
				sb.append(f.getName());
				sb.append("\t");
				sb.append(dt.getOffset(f));
				sb.append("\n");
			}
			
			sb.append("\n");
			
		}
		
		for (LIRMethod method : lirclass.getMethods()) {
			
			if (((Method)method.getAssociactedICNode()).getName().equals("main")) {
				//semantic checks already made sure there is but
				//one method by the name main and is has the correct
				//signature:
				mainMethod = method;
				continue;
			}

			sb.append(method.accept(this));
			
		}
				
		return sb.toString();
	}

	@Override
	public Object visit(LIRMethod method) {
				
		StringBuilder sb = new StringBuilder();
		
		for (LIRInstruction instruction : method.getInstructions()) {
			sb.append(instruction.accept(this));
		}
		
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(ConstantInteger constant) {
		return (constant.getValue() + "");
	}

	@Override
	public Object visit(ConstantNull constant) {
		return (constant.getValue() + "");
	}
	
	@Override
	public Object visit(ConstantString constant) {
		// these don't really exists...........................
		return null;
	}

	@Override
	public Object visit(ConstantDispatch constant) {
		// these don't really exists...........................
		return null;
	}

	@Override
	public Object visit(Register register) {
		if (register.getNum() == Register.DUMMY)
			return "Rdummy";
		return "R" + register.getNum();
	}

	@Override
	public Object visit(Memory memory) {
		return memory.getVariableName();
	}

	@Override
	public Object visit(Label label) {
		
		/*
		 * Possible optimization:
		 * 		label1:
		 * 		label2:
		 * 	merge them to one label
		 */

		StringBuilder sb = new StringBuilder();

		sb.append(label.getValue());
		sb.append(":");
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(ArrayLocation location) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(location.getArray().accept(this));
		sb.append("[");
		sb.append(location.getLocation().accept(this));
		sb.append("]");
		
		return sb.toString();
	}

	@Override
	public Object visit(RegisterOffset offset) {

		StringBuilder sb = new StringBuilder();
		
		sb.append(offset.getRegister().accept(this));
		sb.append(".");
		sb.append(offset.getOffset().accept(this));
		
		return sb.toString();
		
	}

	@Override
	public Object visit(Move move) {

		StringBuilder sb = new StringBuilder();

		sb.append("Move ");
		sb.append(move.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(move.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(ArrayLoad load) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("MoveArray ");
		sb.append(load.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(load.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
		
	}

	@Override
	public Object visit(ArrayStore store) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("MoveArray ");
		sb.append(store.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(store.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(FieldLoad load) {
	
		StringBuilder sb = new StringBuilder();
		
		sb.append("MoveField ");
		sb.append(load.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(load.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
		
	}

	@Override
	public Object visit(FieldStore store) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("MoveField ");
		sb.append(store.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(store.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();

	}

	@Override
	public Object visit(ArrayLength length) {

		StringBuilder sb = new StringBuilder();
		
		sb.append("ArrayLength ");
		sb.append(length.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(length.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(BinaryArithmetic binaryOp) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(binaryOp.getOperation().getDescription());
		sb.append(" ");
		sb.append(binaryOp.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(binaryOp.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(BinaryLogical binaryOp) {

		StringBuilder sb = new StringBuilder();
		
		sb.append(binaryOp.getOperation().getDescription());
		sb.append(" ");
		sb.append(binaryOp.getFirstOperand().accept(this));
		sb.append(",");
		sb.append(binaryOp.getSecondOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(UnaryArithmetic unaryOp) {

		StringBuilder sb = new StringBuilder();
		
		sb.append(unaryOp.getOperation().getDescription());
		sb.append(" ");
		sb.append(unaryOp.getOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(UnaryLogical unaryOp) {

		/* Possible optimization:
		 * 		!!!!!!!!something
		 * would be just be a long list of NOT Rx, always
		 * the same x. count them and either remove is even
		 * or replace with just one if odd.
		 */
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(unaryOp.getOperation().getDescription());
		sb.append(" ");
		sb.append(unaryOp.getOperand().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(Jump jump) {

		/*
		 * Possible optimization:
		 * 		Jump xLabel
		 * 		xLabel:
		 * 		Jump yLabel
		 *  jump do Jump yLabel on the first jump
		 *  
		 *  Possible optimization:
		 *  	Move 0,R1
		 *  	Compare R2,R1
		 *  	JumpFalse label
		 *  simply compare R2 to 0 (discard the move). only works with registers.
		 *  
		 *  Possible optimization:
		 *  	while(true)
		 *  	while(false)
		 *  	if(true)
		 *  	else (true)
		 *  no need to translate condition
		 */
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(jump.getOperation().getDescription());
		sb.append(" ");
		sb.append(jump.getLabel().getValue());
		sb.append("\n");
		
		return sb.toString();

	}

	@Override
	public Object visit(LibraryCall call) {

		StringBuilder sb = new StringBuilder();
		
		sb.append("Library ");
		sb.append(call.getName());
		sb.append("(");
		
		boolean first = true;
		for (BasicOperand op : call.getParameters()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			sb.append(op.accept(this));
		}
		
		sb.append("),");
		sb.append(call.getReturnRegister().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(StaticCall call) {

		StringBuilder sb = new StringBuilder();
		
		sb.append("StaticCall ");
		sb.append(call.getName());
		sb.append("(");
		
		boolean first = true;
		for (Memory mem : call.getParameters().keySet()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			sb.append(mem.accept(this));
			sb.append("=");
			sb.append(call.getParameter(mem).accept(this));
		}
		
		sb.append("),");
		sb.append(call.getReturnRegister().accept(this));
		sb.append("\n");
		
		return sb.toString();
	
	}

	@Override
	public Object visit(VirtualCall call) {

		StringBuilder sb = new StringBuilder();
		
		sb.append("VirtualCall ");
		sb.append(call.getOffset().accept(this));
		sb.append("(");
		
		boolean first = true;
		for (Memory mem : call.getParameters().keySet()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			sb.append(mem.accept(this));
			sb.append("=");
			sb.append(call.getParameter(mem).accept(this));
		}
		
		sb.append("),");
		sb.append(call.getReturnRegister().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public Object visit(Return returnInstruction) {
		
		/* Possible optimization:
		 * 		Return x
		 * 		Jump..
		 * Remove the jump (happens usually at end of if with else)
		 */
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Return ");
		sb.append(returnInstruction.getValue().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

}
