package IC.Semantics.Scopes;

import IC.AST.Formal;
import IC.AST.Method;
import IC.AST.PrimitiveType;
import IC.AST.UserType;


public class ScopesPrinter implements ScopesVisitor {

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
		
		output.append("Children tables: ");
		boolean first = true;
		for (Scope scope : program.getChildrenScopes()) {

			if (first)
				first = false;
			else
				output.append(", ");
			
			output.append(scope.getID());
		}
		
		output.append("\n"); //end line with line separator
		output.append("\n"); //separate between tables
		
		for (Scope scope : program.getChildrenScopes()) {
			output.append(scope.accept(this));
			output.append("\n");
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
			if (sym.getType().equals(Type.THIS))
				continue;
			output.append("\t");
			output.append(sym.getKind().getValue());
			output.append(": ");
			output.append(sym.getID());
			
			if (sym.getKind().equals(Kind.STATICMETHOD) ||
					sym.getKind().equals(Kind.STATICMETHOD)) {

				output.append(" {");
				
				boolean first = true;
				for(Formal f : ((Method)sym.getNode()).getFormals()) {
					
					if (first)
						first = false;
					else
						output.append(", ");
					
					if (f.getType() instanceof PrimitiveType)
						output.append(((PrimitiveType)f.getType()).getName());
					else
						output.append(((UserType)f.getType()).getName());
				}
				
				output.append(" -> ");
				output.append(sym.getType().getValue());
				output.append("}");
			}
			
			output.append("\n");			
		}
		
		output.append("Children tables: ");
		boolean first = true;
		for (Scope scope : icClass.getChildrenScopes()) {

			if (first)
				first = false;
			else
				output.append(", ");
			
			output.append(scope.getID());
		}
		
		output.append("\n"); //end line with line separator
		output.append("\n");
		
		for (Scope scope : icClass.getChildrenScopes()) {
			output.append(scope.accept(this));
			output.append("\n");
		}
		
		return output.toString();
	}

	@Override
	public Object visit(BlockScope block) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Scope scope) {
		// TODO Auto-generated method stub
		return null;
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
			output.append(sym.getID());
			
		}
		
		output.append("Children tables: ");
		boolean first = true;
		for (Scope scope : method.getChildrenScopes()) {

			if (first)
				first = false;
			else
				output.append(", ");
			
			output.append(scope.getID());
		}
		
		output.append("\n"); //end line with line separator
		
		return output.toString();
	}

	@Override
	public Object visit(PrimitiveType type) {
		return type.getName();
	}


}
