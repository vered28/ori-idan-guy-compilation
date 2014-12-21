package IC.Semantics.Types;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import IC.DataTypes;

public class TypeTable {

	private HashMap<String, TypeEntry> table;
	private String filename;
	
	public TypeTable(String filename) {
		this.filename = filename;
		this.table = new HashMap<String, TypeEntry>();
	}
	
	/**
	 * puts non-array primitive type in type table (for array type use the
	 * specific (String, Type) method, to handle adding of various
	 * dimensions).
	 */
	public void putPrimitive(String name, DataTypes datatype) {
		if (!table.containsKey(name)) {
			table.put(name, new PrimitiveTypeEntry(name, datatype));
		}
	}
	
	/**
	 * puts class type in type table, associating it with its'
	 * proper ICClass node.
	 */
	public void putClassType(String name, UserType usertype) {
		if (!table.containsKey(name)) {
			table.put(name, new ClassTypeEntry(name, usertype));
		}
	}
	
	/**
	 * puts name mapped to type in type table. DOES NOT handle methods.
	 * To add a method to the type table, use the put(MethodType) overload.
	 */
	public void put(String name, Type type) {
		
		if (!table.containsKey(name)) {
			
			//if type is array-type, first enter all lower dimension types.
			if (type.getDimension() > 0) {
				
				int firstBracket = name.indexOf('[');
				String typeName = name.substring(0, firstBracket);
				
				//make sure regular type (non-array) is in type table.
				//if not, it is class type, since primitives are added
				//before the AST is checked.
				
				if (!table.containsKey(typeName)) {
					table.put(typeName, new ClassTypeEntry(
							typeName, (UserType)type));
				}
				
				String arrayTypeName = typeName;
				
				for (int i = 1; i <= type.getDimension(); i++) {
					
					arrayTypeName += "[]";
					
					if (!table.containsKey(arrayTypeName)) {
						table.put(arrayTypeName, new ArrayTypeEntry(
											arrayTypeName, table.get(typeName)));
					}
				}
				
			} else {
				
				if (type instanceof PrimitiveType)
					putPrimitive(name, ((PrimitiveType)type).getType());
				else if (type instanceof UserType)
					putClassType(name, ((UserType)type));
				
				//use the specific method-type overload to handle correct method name parsing
				
			}
			
		}
		
	}
	
	public void put(MethodType type) {
		if (!table.containsKey(type.getName())) {
			table.put(type.getName(),
					new MethodTypeEntry(type.getName()));
		}
	}
	
	public TypeEntry get(String name) {
		return table.get(name);
	}
	
	public TypeEntry[] getAllTypes() {
		
		TypeEntry[] entries = table.values().toArray(new TypeEntry[0]);
		Arrays.sort(entries, new Comparator<TypeEntry>() {

					@Override
					public int compare(TypeEntry o1, TypeEntry o2) {

						int p1 = o1.getType().getPrecedence();
						int p2 = o2.getType().getPrecedence();
						
						if (p1 == p2) {
							//same precedence, compare by serial id:
							return o1.getSerialID() - o2.getSerialID();
						}
						
						return p1 - p2; //set by the lower precedence first
					}
				});
		
		return entries;
		
	}
	
	public String getFilename() {
		return filename;
	}
	
}
