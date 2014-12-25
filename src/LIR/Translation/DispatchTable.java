package LIR.Translation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import IC.AST.Field;
import IC.AST.Method;

public class DispatchTable implements Cloneable {

	private Map<Field, Integer> fields;
	private Map<Method, Integer> methods;

	private String name;
	
	public DispatchTable(String name) {
		this.name = name;
		this.fields = new HashMap<Field, Integer>();
		this.methods = new HashMap<Method, Integer>();
	}
	
	public String getName() {
		return "_DV_" + name;
	}
	
	public Set<Method> getMethods() {
		return methods.keySet();
	}
	
	public Set<Field> getFields() {
		return fields.keySet();
	}
	
	public void addMethod(Method method) {
		methods.put(method, methods.size());
	}
	
	public void addField(Field field) {
		//0 is actually DV_PTR, so start offsets at 1 not 0
		fields.put(field, fields.size() + 1);
	}

	private void addFields(Map<Field, Integer> values) {
		fields.putAll(values);
	}

	private void addMethods(Map<Method, Integer> values) {
		methods.putAll(values);
	}
	
	@Override
	protected Object clone() {
		DispatchTable clonedObj = new DispatchTable(name);
		clonedObj.addFields(fields);
		clonedObj.addMethods(methods);
		return clonedObj;
	}

}
