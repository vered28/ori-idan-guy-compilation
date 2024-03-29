package IC.Semantics.Scopes;

import IC.Semantics.Types.MethodType;
import IC.Semantics.Types.PrimitiveType;
import IC.Semantics.Types.UserType;

public interface ScopesVisitor {

	public Object visit(ProgramScope program);	

	public Object visit(ClassScope icClass);	

	public Object visit(BlockScope block);
	
	public Object visit(MethodScope method);
	
	public Object visit(PrimitiveType type);

	public Object visit(UserType type);
	
	public Object visit(MethodType type);

	public Object visit(IC.AST.Type type);

}
