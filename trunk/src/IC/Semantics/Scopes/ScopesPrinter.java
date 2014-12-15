package IC.Semantics.Scopes;

import IC.AST.Formal;
import IC.AST.Method;

public class ScopesPrinter implements ScopesVisitor {

	private String printChildrenTables(Scope scope) {
		
		StringBuffer output = new StringBuffer();
		
		if (scope.getChildrenScopes().size() > 0) {

			output.append("Children tables: ");
			boolean first = true;
			for (Scope childScope : scope.getChildrenScopes()) {

				if (first)
					first = false;
				else
					output.append(", ");
				
				output.append(childScope.getID());
			}
			
			output.append("\n"); //end line with line separator
			
		}
	
		return output.toString();
	}
	
	@Override
	public Object visit(final ProgramScope program) {
		
		StringBuffer output = new StringBuffer();
		
		output.append("Global Symbol Table: ");
		output.append(program.getID());
		output.append("\n");
		
		for (Symbol sym : program.getSymbols()) {
			output.append("\t");
			output.append(sym.getKind().getValue());
			output.append(": ");
			output.append(sym.getID());
			output.append("\n");
		}
		
		output.append(printChildrenTables(program));
		
		for (Scope scope : program.getChildrenScopes()) {
			output.append("\n");
			output.append(scope.accept(this));
		}
		
		return output.toString();
		
	}

	@Override
	public Object visit(ClassScope icClass) {
		
		StringBuffer output = new StringBuffer();
		
		output.append("Class Symbol Table: ");
		output.append(icClass.getID());
		output.append("\n");
		
		for (Symbol sym : icClass.getSymbols()) {
						
			output.append("\t");
			output.append(sym.getKind().getValue());
			output.append(": ");
			output.append(sym.getID());
			
			if (Kind.isMethod(sym.getKind())) {

				output.append(" {");
				
				boolean first = true;
				for(Formal formal : ((Method)sym.getNode()).getFormals()) {
					
					if (first)
						first = false;
					else
						output.append(", ");
					
					output.append(sym.getType().accept(this, formal.getType()));
				}
				
				output.append(" -> ");
				output.append(sym.getType().accept(this, ((Method)sym.getNode()).getType()));
				output.append("}");
			}
			
			output.append("\n");			
		}
		
		output.append(printChildrenTables(icClass));
				
		for (Scope scope : icClass.getChildrenScopes()) {
			output.append("\n");
			output.append(scope.accept(this));
		}
		
		return output.toString();
	}

	@Override
	public Object visit(BlockScope block) {

		StringBuffer output = new StringBuffer();
		
		output.append("Statement Block Symbol Table ( located in ");
		output.append(block.getParentScope().getID());
		output.append(" )\n");
		
		for (Symbol sym : block.getSymbols()) {
			output.append("\t");
			output.append(sym.getKind().getValue());
			output.append(": ");
			output.append(sym.getType().accept(this));
			output.append(" ");
			output.append(sym.getID());
			output.append("\n");
		}
		
		output.append(printChildrenTables(block));
		
		for (Scope scope : block.getChildrenScopes()) {
			output.append("\n");
			output.append(scope.accept(this));
		}

		return output.toString();
	}

	@Override
	public Object visit(MethodScope method) {
		
		StringBuffer output = new StringBuffer();
		
		output.append("Method Symbol Table: ");
		output.append(method.getID());
		output.append("\n");
		
		for (Symbol sym : method.getSymbols()) {
			output.append("\t");
			output.append(sym.getKind().getValue());
			output.append(": ");
			output.append(sym.getType().accept(this));
			output.append(" ");
			output.append(sym.getID());
			output.append("\n");
		}
		
		output.append(printChildrenTables(method));
		
		for (Scope scope : method.getChildrenScopes()) {
			output.append("\n");
			output.append(scope.accept(this));
		}

		return output.toString();
	}

	@Override
	public Object visit(IC.AST.Type type) {
		
		StringBuffer output = new StringBuffer();
		
		output.append(type.getName());
		for (int i = 0; i < type.getDimension(); i++) {
			output.append("[]");
		}
		
		return output;
	}

	@Override
	public Object visit(PrimitiveType type) {

		StringBuffer output = new StringBuffer();
		
		output.append(type.getType().getDescription());
		for (int i = 0; i < type.getDimension(); i++) {
			output.append("[]");
		}
		
		return output;

	}

	@Override
	public Object visit(UserType type) {

		StringBuffer output = new StringBuffer();
		
		output.append(type.getName());
		for (int i = 0; i < type.getDimension(); i++) {
			output.append("[]");
		}
		
		return output;
	}

}
