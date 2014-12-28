package LIR.Instructions;

public interface LIRVisitor {
	
	public Object visit(LIRProgram program);
	
	public Object visit(LIRClass lirclass);
	
	public Object visit(LIRMethod method);

	public Object visit(ConstantInteger constant);

	public Object visit(ConstantNull constant);

	public Object visit(ConstantString constant);
	
	public Object visit(ConstantDispatch constant);
	
	public Object visit(Register register);
	
	public Object visit(Memory memory);

	public Object visit(Label label);
	
	public Object visit(ArrayLocation location);
	
	public Object visit(RegisterOffset offset);
		
	public Object visit(Move move);
	
	public Object visit(ArrayLoad load);
	
	public Object visit(ArrayStore store);
	
	public Object visit(FieldLoad load);
	
	public Object visit(FieldStore store);
	
	public Object visit(ArrayLength length);
	
	public Object visit(BinaryArithmetic binaryOp);
	
	public Object visit(BinaryLogical binaryOp);

	public Object visit(UnaryArithmetic unaryOp);
	
	public Object visit(UnaryLogical unaryOp);
	
	public Object visit(Jump jump);
	
	public Object visit(LibraryCall call);

	public Object visit(StaticCall call);

	public Object visit(VirtualCall call);
	
	public Object visit(Return returnInstruction);

}
