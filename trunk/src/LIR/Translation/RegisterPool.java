package LIR.Translation;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import IC.AST.ASTNode;
import LIR.Instructions.Register;

public class RegisterPool {

	private static int counter = 0;
	private static SortedSet<Integer> pool;
	
	private static List<Integer> waiting;
	
	static {
		pool = new TreeSet<Integer>();
		waiting = new LinkedList<Integer>();
	}
	
	public static Register get(ASTNode node) {
		
		if (!pool.isEmpty()) {
			Register reg = new Register(node, pool.first());
			pool.remove(pool.first());
			clearWaiting();
			return reg;
		}
		
		clearWaiting();
		return new Register(node, ++counter);
	}
	
	public static void putback(Register reg) {
		pool.add(reg.getNum());
	}
	
	public static void putbackAfterNextGet(Register reg) {
		waiting.add(reg.getNum());
	}
	
	public static void flushPool() {
		for (int i = 1; i <= counter; i++) {
			pool.add(i);
		}
	}
	
	private static void clearWaiting() {
		for (int i : waiting) { pool.add(i); }
		waiting.clear();
	}	
}
