package IC.AST;

import IC.Semantics.Types.MethodType;


/**
 * Pretty printing visitor - travels along the AST and prints info about each
 * node, in an easy-to-comprehend format.
 * 
 * @author Tovi Almozlino
 */
public class PrettyPrinter implements Visitor {

	private int depth = 0; // depth of indentation

	private String ICFilePath;

	/**
	 * Constructs a new pretty printer visitor.
	 * 
	 * @param ICFilePath
	 *            The path + name of the IC file being compiled.
	 */
	public PrettyPrinter(String ICFilePath) {
		this.ICFilePath = ICFilePath;
	}

	private void indent(StringBuffer output, ASTNode node) {
		output.append("\n");
		for (int i = 0; i < depth; ++i)
			output.append("\t");
		if (node != null)
			output.append(node.getLine() + ": ");
	}

	private void indent(StringBuffer output) {
		indent(output, null);
	}

	@Override
	public Object visit(Program program) {
		StringBuffer output = new StringBuffer();

		indent(output);
		output.append("Abstract Syntax Tree: " + ICFilePath + "\n");
		for (ICClass icClass : program.getClasses())
			output.append(icClass.accept(this));
		return output.toString();
	}

	@Override
	public Object visit(ICClass icClass) {
		StringBuffer output = new StringBuffer();
		
		indent(output, icClass);
		output.append("Declaration of class: " + icClass.getName());
		if (icClass.hasSuperClass())
			output.append(", subclass of " + icClass.getSuperClassName());
		output.append(", Type: ");
		output.append(icClass.getName());
		output.append(", Symbol table: ");
		if (icClass.hasSuperClass())
			output.append(icClass.getEnclosingScope().getParentScope().getID());
		else
			output.append("Global");
		depth++;
		for (Field field : icClass.getFields())
			output.append(field.accept(this));
		for (Method method : icClass.getMethods())
			output.append(method.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(PrimitiveType type) {
		StringBuffer output = new StringBuffer();

		//the example makes it look like this should do nothing...
		
//		indent(output, type);
//		output.append("Primitive data type: ");
//		if (type.getDimension() > 0)
//			output.append(type.getDimension() + "-dimensional array of ");
//		output.append(type.getName());
//		output.append(", Type: ");
//		output.append(type.getName());
//		output.append(", Symbol table: ");
//		output.append(type.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(UserType type) {
		StringBuffer output = new StringBuffer();

		//the example makes it look like this should do nothing...

//		indent(output, type);
//		output.append("User-defined data type: ");
//		if (type.getDimension() > 0)
//			output.append(type.getDimension() + "-dimensional array of ");
//		output.append(type.getName());
//		output.append(", Type: ");
//		output.append(type.getName());
//		output.append(", Symbol table: ");
//		output.append(type.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(Field field) {
		StringBuffer output = new StringBuffer();

		indent(output, field);
		output.append("Declaration of field: " + field.getName());
		output.append(", Type: ");
		output.append(field.getNodeType().getName());
		for (int i = 0; i < field.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(field.getEnclosingScope().getID());
		++depth;
		output.append(field.getType().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(LibraryMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of library method: " + method.getName());
		output.append(", Type: ");
		output.append(new MethodType(method).getName());
		output.append(", Symbol table: ");
		output.append(method.getEnclosingScope().getParentScope().getID());
		depth++;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(Formal formal) {
		StringBuffer output = new StringBuffer();

		indent(output, formal);
		output.append("Parameter: " + formal.getName());
		output.append(", Type: ");
		output.append(formal.getNodeType().getName());
		for (int i = 0; i < formal.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(formal.getEnclosingScope().getID());
		++depth;
		output.append(formal.getType().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(VirtualMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of virtual method: " + method.getName());
		output.append(", Type: ");
		output.append(new MethodType(method).getName());
		output.append(", Symbol table: ");
		output.append(method.getEnclosingScope().getParentScope().getID());
		depth++;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		for (Statement statement : method.getStatements())
			output.append(statement.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(StaticMethod method) {
		StringBuffer output = new StringBuffer();

		indent(output, method);
		output.append("Declaration of static method: " + method.getName());
		output.append(", Type: ");
		output.append(new MethodType(method).getName());
		output.append(", Symbol table: ");
		output.append(method.getEnclosingScope().getParentScope().getID());
		depth++;
		output.append(method.getType().accept(this));
		for (Formal formal : method.getFormals())
			output.append(formal.accept(this));
		for (Statement statement : method.getStatements())
			output.append(statement.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(Assignment assignment) {
		StringBuffer output = new StringBuffer();

		indent(output, assignment);
		output.append("Assignment statement");
		output.append(", Symbol table: ");
		output.append(assignment.getEnclosingScope().getID());
		depth++;
		output.append(assignment.getVariable().accept(this));
		output.append(assignment.getAssignment().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(CallStatement callStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, callStatement);
		output.append("Method call statement");
		output.append(", Type: ");
		output.append(callStatement.getNodeType().getName());
		for (int i = 0; i < callStatement.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(callStatement.getEnclosingScope().getID());
		++depth;
		output.append(callStatement.getCall().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(Return returnStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, returnStatement);
		output.append("Return statement");
		if (returnStatement.hasValue())
			output.append(", with return value");
		output.append(", Symbol table: ");
		output.append(returnStatement.getEnclosingScope().getID());
		if (returnStatement.hasValue()) {
			++depth;
			output.append(returnStatement.getValue().accept(this));
			--depth;
		}
		return output.toString();
	}

	@Override
	public Object visit(If ifStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, ifStatement);
		output.append("If statement");
		if (ifStatement.hasElse())
			output.append(", with Else operation");
		output.append(", Symbol table: ");
		output.append(ifStatement.getEnclosingScope().getID());
		depth++;
		output.append(ifStatement.getCondition().accept(this));
		output.append(ifStatement.getOperation().accept(this));
		if (ifStatement.hasElse())
			output.append(ifStatement.getElseOperation().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(While whileStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, whileStatement);
		output.append("While statement");
		output.append(", Symbol table: ");
		output.append(whileStatement.getEnclosingScope().getID());
		depth++;
		output.append(whileStatement.getCondition().accept(this));
		output.append(whileStatement.getOperation().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(Break breakStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, breakStatement);
		output.append("Break statement");
		output.append(", Symbol table: ");
		output.append(breakStatement.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(Continue continueStatement) {
		StringBuffer output = new StringBuffer();

		indent(output, continueStatement);
		output.append("Continue statement");
		output.append(", Symbol table: ");
		output.append(continueStatement.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) {
		StringBuffer output = new StringBuffer();

		indent(output, statementsBlock);
		output.append("Block of statements");
		output.append(", Symbol table: ");
		output.append(statementsBlock.getEnclosingScope().getID());
		depth++;
		for (Statement statement : statementsBlock.getStatements())
			output.append(statement.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(LocalVariable localVariable) {
		StringBuffer output = new StringBuffer();

		indent(output, localVariable);
		output.append("Declaration of local variable: "
				+ localVariable.getName());
		if (localVariable.hasInitValue()) {
			output.append(", with initial value");
			++depth;
		}
		output.append(", Type: ");
		output.append(localVariable.getNodeType().getName());
		for (int i = 0; i < localVariable.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(localVariable.getEnclosingScope().getID());
		++depth;
		output.append(localVariable.getType().accept(this));
		if (localVariable.hasInitValue()) {
			output.append(localVariable.getInitValue().accept(this));
			--depth;
		}
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(VariableLocation location) {
		StringBuffer output = new StringBuffer();

		indent(output, location);
		output.append("Reference to variable: " + location.getName());
		if (location.isExternal())
			output.append(", in external scope");
		output.append(", Type: ");
		output.append(location.getNodeType().getName());
		for (int i = 0; i < location.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(location.getEnclosingScope().getID());
		if (location.isExternal()) {
			++depth;
			output.append(location.getLocation().accept(this));
			--depth;
		}
		return output.toString();
	}

	@Override
	public Object visit(ArrayLocation location) {
		StringBuffer output = new StringBuffer();

		indent(output, location);
		output.append("Reference to array");
		output.append(", Type: ");
		output.append(location.getNodeType().getName());
		for (int i = 0; i < location.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(location.getEnclosingScope().getID());
		depth++;
		output.append(location.getArray().accept(this));
		output.append(location.getIndex().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(StaticCall call) {
		StringBuffer output = new StringBuffer();

		indent(output, call);
		output.append("Call to static method: " + call.getName()
				+ ", in class " + call.getClassName());
		output.append(", Type: ");
		output.append(call.getNodeType().getName());
		for (int i = 0; i < call.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(call.getEnclosingScope().getID());
		depth++;
		for (Expression argument : call.getArguments())
			output.append(argument.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(VirtualCall call) {
		StringBuffer output = new StringBuffer();

		indent(output, call);
		output.append("Call to virtual method: " + call.getName());
		if (call.isExternal())
			output.append(", in external scope");
		output.append(", Type: ");
		output.append(call.getNodeType().getName());
		for (int i = 0; i < call.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(call.getEnclosingScope().getID());
		depth++;
		if (call.isExternal())
			output.append(call.getLocation().accept(this));
		for (Expression argument : call.getArguments())
			output.append(argument.accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(This thisExpression) {
		StringBuffer output = new StringBuffer();

		indent(output, thisExpression);
		output.append("Reference to 'this' instance");
		output.append(", Type: ");
		output.append(thisExpression.getNodeType().getName());
		output.append(", Symbol table: ");
		output.append(thisExpression.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(NewClass newClass) {
		StringBuffer output = new StringBuffer();

		indent(output, newClass);
		output.append("Instantiation of class: " + newClass.getName());
		output.append(", Type: ");
		output.append(newClass.getNodeType().getName());
		output.append(", Symbol table: ");
		output.append(newClass.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(NewArray newArray) {
		StringBuffer output = new StringBuffer();

		indent(output, newArray);
		output.append("Array allocation");
		output.append(", Type: ");
		output.append(newArray.getNodeType().getName());
		for (int i = 0; i < newArray.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(newArray.getEnclosingScope().getID());
		depth++;
		output.append(newArray.getType().accept(this));
		output.append(newArray.getSize().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(Length length) {
		StringBuffer output = new StringBuffer();

		indent(output, length);
		output.append("Reference to array length");
		output.append(", Type: ");
		output.append(length.getNodeType().getName());
		for (int i = 0; i < length.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(length.getEnclosingScope().getID());
		++depth;
		output.append(length.getArray().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, binaryOp);
		output.append("Mathematical binary operation: "
				+ binaryOp.getOperator().getDescription());
		output.append(", Type: ");
		output.append(binaryOp.getNodeType().getName());
		output.append(", Symbol table: ");
		output.append(binaryOp.getEnclosingScope().getID());
		depth++;
		output.append(binaryOp.getFirstOperand().accept(this));
		output.append(binaryOp.getSecondOperand().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, binaryOp);
		output.append("Logical binary operation: "
				+ binaryOp.getOperator().getDescription());
		output.append(", Type: ");
		output.append(binaryOp.getNodeType().getName());
		for (int i = 0; i < binaryOp.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(binaryOp.getEnclosingScope().getID());
		depth++;
		output.append(binaryOp.getFirstOperand().accept(this));
		output.append(binaryOp.getSecondOperand().accept(this));
		depth--;
		return output.toString();
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, unaryOp);
		output.append("Mathematical unary operation: "
				+ unaryOp.getOperator().getDescription());
		output.append(", Type: ");
		output.append(unaryOp.getNodeType().getName());
		for (int i = 0; i < unaryOp.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(unaryOp.getEnclosingScope().getID());
		++depth;
		output.append(unaryOp.getOperand().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		StringBuffer output = new StringBuffer();

		indent(output, unaryOp);
		output.append("Logical unary operation: "
				+ unaryOp.getOperator().getDescription());
		output.append(", Type: ");
		output.append(unaryOp.getNodeType().getName());
		for (int i = 0; i < unaryOp.getNodeType().getDimension(); i++)
			output.append("[]");
		output.append(", Symbol table: ");
		output.append(unaryOp.getEnclosingScope().getID());
		++depth;
		output.append(unaryOp.getOperand().accept(this));
		--depth;
		return output.toString();
	}

	@Override
	public Object visit(Literal literal) {
		StringBuffer output = new StringBuffer();

		indent(output, literal);
		output.append(literal.getType().getDescription() + ": "
				+ literal.getType().toFormattedString(literal.getValue()));
		output.append(", Type: ");
		output.append(literal.getNodeType().getName());
		output.append(", Symbol table: ");
		output.append(literal.getEnclosingScope().getID());
		return output.toString();
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		StringBuffer output = new StringBuffer();

		indent(output, expressionBlock);
		output.append("Parenthesized expression");
		output.append(", Type: ");
		output.append(expressionBlock.getNodeType().getName());
		output.append(", Symbol table: ");
		output.append(expressionBlock.getEnclosingScope().getID());
		++depth;
		output.append(expressionBlock.getExpression().accept(this));
		--depth;
		return output.toString();
	}
}