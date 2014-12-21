package IC.Semantics.Exceptions;

import IC.AST.ASTNode;

public class DeadCodeException extends SemanticError {

	public DeadCodeException(String message, ASTNode node) {
		super("Dead code error: " + message, node);
	}

}
