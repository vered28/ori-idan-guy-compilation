package LIR.Translation;

import java.util.HashMap;
import java.util.Map;

import IC.AST.ASTNode;
import IC.AST.ICClass;
import IC.AST.Method;
import IC.AST.StaticMethod;
import LIR.Instructions.Label;

public class LabelMaker {

	private static String IC_MAIN = "_ic_main";
	private static Label mainLabel = null;
	
	private static Map<String, Integer> labels;
	
	static {
		labels = new HashMap<String, Integer>();
		labels.put(IC_MAIN, 0);
	}
	
	public static Label get(ASTNode node, String label) {
		
		if (label.equals(IC_MAIN))
			return null;
		
		if (!labels.containsKey(label)) {
			labels.put(label, 0);
			return new Label(node, label);
		}
		
		int num = labels.get(label) + 1;
		labels.put(label, num);
		return new Label(node, label + num);
		
	}
	
	public static String methodString(ICClass icClass, Method method) {
		String suffix = "";
		if (method instanceof StaticMethod)
			suffix += "_static";
		//because a class may have a virtual and a static method
		//with the same name
		return "_" + icClass.getName() + "_" + method.getName() + suffix;
	}
	
	public static String labelString(ICClass icClass, Method method, String basicLabel) {
		return methodString(icClass, method) + basicLabel;
	}
		
	public static Label getMainLabel(StaticMethod main) {
		if (mainLabel != null)
			return mainLabel;
		return (mainLabel = new Label(main, IC_MAIN));
	}
	
}
