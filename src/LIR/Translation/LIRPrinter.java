package LIR.Translation;

import IC.AST.ICClass;
import IC.AST.Method;
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

	@Override
	public Object visit(LIRProgram program) {

		StringBuilder sb = new StringBuilder();
		
		//first print string literals:
		for (StringLiteral literal : program.getLiterals()) {
			sb.append(literal.getId());
			sb.append(": \"");
			sb.append(literal.getLiteral().getValue());
			sb.append("\"\n");
		}
				
		//next, print dispatch tables:
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
				
				sb.append(LabelMaker.methodString(icClass, method));
				
			}
			
			sb.append("]");
			sb.append("\n");
			
		}
		
		sb.append("\n");
		
		for (LIRClass lirclass : program.getClasses()) {
			if (((ICClass)lirclass.getAssociactedICNode()).getName().equals("LIbrary"))
				continue;
			sb.append(lirclass.accept(this));
		}

		return sb.toString();
	}

	@Override
	public Object visit(LIRClass lirclass) {

		StringBuilder sb = new StringBuilder();
		
		for (LIRMethod method : lirclass.getMethods()) {
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
		return "null";
	}
	
	@Override
	public Object visit(ConstantString constant) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(ConstantDispatch constant) {
		// TODO Auto-generated method stub.
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
		// TODO Auto-generated method stub.
		return null;
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
		// TODO Auto-generated method stub.
		return null;
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
		// TODO Auto-generated method stub.
		return null;
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
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(UnaryLogical unaryOp) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(Jump jump) {

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
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		// TODO Auto-generated method stub.
		return null;
	}

	@Override
	public Object visit(Return returnInstruction) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Return ");
		sb.append(returnInstruction.getValue().accept(this));
		sb.append("\n");
		
		return sb.toString();
	}

}
