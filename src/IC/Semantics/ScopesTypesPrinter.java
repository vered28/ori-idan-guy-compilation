package IC.Semantics;

import IC.AST.ICClass;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Scopes.ScopesPrinter;
import IC.Semantics.Scopes.Symbol;
import IC.Semantics.Types.TypeEntry;
import IC.Semantics.Types.TypeTable;
import IC.Semantics.Types.TypesEnum;

public class ScopesTypesPrinter {

	private ProgramScope mainScope;
	private TypeTable typeTable;
	
	public ScopesTypesPrinter(ProgramScope mainScope, TypeTable typeTable) {
		this.mainScope = mainScope;
		this.typeTable = typeTable;
	}
	
	public ScopesTypesPrinter(ScopesTypesWrapper wrapper) {
		this.mainScope = wrapper.getGlobalScope();
		this.typeTable = wrapper.getTypeTable();
	}
	
	public void print() {
		
		mainScope.accept(new ScopesPrinter());
		
		printTypeTable();
	}
	
	private String getSuperClassName(String className) {
		Symbol classSymbol = mainScope.getSymbol(className);
		if (classSymbol == null)
			return null;
		
		return ((ICClass)classSymbol.getNode()).getSuperClassName();
	}
	
	private void printTypeTable() {
		
		System.out.println("Type Table: " + typeTable.getFilename());
		
		TypeEntry[] entries = typeTable.getAllTypes();
		for (TypeEntry entry : entries) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("\t");
			sb.append(entry.getSerialID());
			sb.append(": ");
			sb.append(entry.getType().getDescription());
			sb.append(": ");
			sb.append(entry.getName());
			
			if (entry.getType().equals(TypesEnum.ClassType)) {
				//find if there's a super class
				String superClass = getSuperClassName(entry.getName());
				if (superClass != null) {
					sb.append(", Superclass ID: ");
					sb.append(typeTable.get(superClass).getSerialID());
				}
			}
			
			System.out.println(sb.toString());
		}		
	}
	
}
