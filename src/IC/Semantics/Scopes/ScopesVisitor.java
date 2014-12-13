package IC.Semantics.Scopes;

import IC.AST.Formal;
import IC.AST.PrimitiveType;

public interface ScopesVisitor {

	public Object visit(ProgramScope program);	

	public Object visit(ClassScope icClass);	

	public Object visit(BlockScope block);
	
	public Object visit(MethodScope method);

	public Object visit(Scope scope);	
	
	public Object visit(PrimitiveType type);

}
