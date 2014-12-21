package IC.Semantics.Validations;

import IC.DataTypes;
import IC.AST.ASTNode;
import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.CallStatement;
import IC.AST.Continue;
import IC.AST.ExpressionBlock;
import IC.AST.Field;
import IC.AST.Formal;
import IC.AST.ICClass;
import IC.AST.If;
import IC.AST.Length;
import IC.AST.LibraryMethod;
import IC.AST.Literal;
import IC.AST.LocalVariable;
import IC.AST.LogicalBinaryOp;
import IC.AST.LogicalUnaryOp;
import IC.AST.MathBinaryOp;
import IC.AST.MathUnaryOp;
import IC.AST.Method;
import IC.AST.NewArray;
import IC.AST.NewClass;
import IC.AST.PrimitiveType;
import IC.AST.Program;
import IC.AST.Return;
import IC.AST.StatementsBlock;
import IC.AST.StaticCall;
import IC.AST.StaticMethod;
import IC.AST.This;
import IC.AST.Type;
import IC.AST.UserType;
import IC.AST.VariableLocation;
import IC.AST.VirtualCall;
import IC.AST.VirtualMethod;
import IC.AST.Visitor;
import IC.AST.While;
import IC.Semantics.Exceptions.SemanticError;

public class SingleMainValidation implements Visitor {

	private ASTNode virtualMain;
	private ASTNode libraryMain;
	private ASTNode incorrectStatic;
	private int staticMainCount = 0;
	
	@Override
	public Object visit(Program program) {

		for (ICClass cls : program.getClasses()) {
			cls.accept(this);
		}
		
		/* after going through all methods, we can have a better
		 * error messaging, if one such error has occurred (in other
		 * words, we'll give precedence to the more important error
		 * rather than the first one we encounter).
		 */
		
		if (staticMainCount > 1) {
			throw new SemanticError("Program has more than one main method.", program);
		}
			
		//staticMainCount is either 0 or 1. We have to make sure
		//no other problematic main methods were defined / declared:
		
		if (incorrectStatic != null) {
			if (staticMainCount == 0)
				throw new SemanticError("Incorrect main method signature", incorrectStatic);
			throw new SemanticError("Program has more than one main method, one with incorrect signature.", incorrectStatic);
		}
			
		if (libraryMain != null) {
			throw new SemanticError("Library defines a main method (main signature requires implemenation for point of entry).", libraryMain);
		}
		
		if (virtualMain != null) {
			if (staticMainCount == 0)
				throw new SemanticError("Incorrect main method signature, cannot be virtual!", virtualMain);
			throw new SemanticError("Program has more than one main method, one of which is virtual.", virtualMain);
		}
		
		//no exception thrown... everything is O.K. :)
		
		return null;
	}

	@Override
	public Object visit(ICClass icClass) {

		for (Method method : icClass.getMethods()) {
			method.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(Field field) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VirtualMethod method) {
		
		if (method.getName().equals("main"))
			if (virtualMain == null)
				virtualMain = method;
		
		return null;
	}

	@Override
	public Object visit(StaticMethod method) {
		
		if (method.getName().equals("main")) {
			//V static *** main(**)
			
			if (method.getType() instanceof PrimitiveType &&
					((PrimitiveType)method.getType()).getPrimitiveType() == DataTypes.VOID) {
				//V static void main(**)
				if (method.getFormals().size() == 1) {
					Type formalType = method.getFormals().get(0).getType();
					if (formalType instanceof PrimitiveType &&
							((PrimitiveType)formalType).getPrimitiveType() == DataTypes.STRING
							&& formalType.getDimension() == 1) {
							//V static void main(string[])
							staticMainCount++;
							return null;
						}							
				}
			}
			
			//reaching this point means there's a static method
			//main that has an incorrect signature :(
			if (incorrectStatic == null)
				incorrectStatic = method;
		}
		
		return null;
	}

	@Override
	public Object visit(LibraryMethod method) {

		if (method.getName().equals("main"))
			if (libraryMain == null)
				libraryMain = method;

		return null;
	}

	@Override
	public Object visit(Formal formal) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(PrimitiveType type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(UserType type) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Assignment assignment) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(CallStatement callStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Return returnStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(If ifStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(While whileStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Break breakStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Continue continueStatement) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(StatementsBlock statementsBlock) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(LocalVariable localVariable) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VariableLocation location) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(ArrayLocation location) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(StaticCall call) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(VirtualCall call) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(This thisExpression) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(NewClass newClass) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(NewArray newArray) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Length length) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MathBinaryOp binaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(LogicalBinaryOp binaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(MathUnaryOp unaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(LogicalUnaryOp unaryOp) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(Literal literal) {
		//do nothing
		return null;
	}

	@Override
	public Object visit(ExpressionBlock expressionBlock) {
		//do nothing
		return null;
	}

}
