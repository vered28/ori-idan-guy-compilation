package LIR.Translation;

import java.util.SortedSet;
import java.util.TreeSet;

import IC.AST.ASTNode;
import LIR.Instructions.Register;

public class RegisterPool {

	private static int counter = 0;
	private static SortedSet<Integer> pool;
	
	static {
		pool = new TreeSet<Integer>();
	}
	
	public static Register get(ASTNode node) {
		
		if (!pool.isEmpty()) {
			Register reg = new Register(node, pool.first());
			pool.remove(pool.first());
			return reg;
		}
		
		return new Register(node, ++counter);
	}
	
	public static void putback(Register reg) {
		pool.add(reg.getNum());
	}
}
