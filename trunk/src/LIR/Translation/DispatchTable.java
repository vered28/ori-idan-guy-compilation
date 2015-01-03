package LIR.Translation;

import java.util.LinkedList;
import java.util.List;

import IC.AST.Field;
import IC.AST.Method;

public class DispatchTable implements Cloneable {

	private List<Field> fields;
	private List<Method> methods;

	private String name;
	
	public DispatchTable(String name) {
		this.name = name;
		this.fields = new LinkedList<Field>();
		this.methods = new LinkedList<Method>();
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return "_DV_" + name;
	}
	
	public List<Method> getMethods() {
		return methods;
	}
	
	public List<Field> getFields() {
		return fields;
	}
	
	public int getOffset(Field field) {
		for (int i = 0; i < fields.size(); i++) {
			if (field == fields.get(i))
				return i+1; //account for 0 for DV_PTR
		}
		
		return 0;
	}

	public int getOffset(Method method) {
		for (int i = 0; i < methods.size(); i++) {
			if (method == methods.get(i))
				return i;
		}
		
		return 0;
	}

	public void addMethod(Method method) {
		methods.add(method);
	}
	
	public void addField(Field field) {
		//0 is actually DV_PTR, so start offsets at 1 not 0
		fields.add(field);
	}

	private void addFields(List<Field> values) {
		fields.addAll(values);
	}

	private void addMethods(List<Method> values) {
		methods.addAll(values);
	}
	
	@Override
	protected Object clone() {
		DispatchTable clonedObj = new DispatchTable(name);
		clonedObj.addFields(fields);
		clonedObj.addMethods(methods);
		return clonedObj;
	}

}
