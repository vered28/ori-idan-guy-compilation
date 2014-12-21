package IC.Semantics.Exceptions;

import IC.AST.ASTNode;
import IC.Semantics.Types.Type;

public class TypeMismatchSemanticError extends SemanticError {

	private static final long serialVersionUID = -2486072882050195443L;

	public TypeMismatchSemanticError(Type fromNodeType, Type toNodeType, ASTNode node) {
		super("Type mismatch: cannot convert from type " +
				fromNodeType + " to type " + toNodeType, node);
	}

}
