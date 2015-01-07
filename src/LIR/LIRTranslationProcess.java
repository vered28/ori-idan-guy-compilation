package LIR;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import IC.AST.ICClass;
import IC.AST.Program;
import LIR.Instructions.LIRProgram;
import LIR.Translation.BuildGlobalConstants;
import LIR.Translation.DispatchTable;
import LIR.Translation.GlobalConstantsWrapper;
import LIR.Translation.LIRPrinter;
import LIR.Translation.StringLiteralSet;
import LIR.Translation.SymbolLastUsed;
import LIR.Translation.TranslateIC2LIR;

public class LIRTranslationProcess {

	private Program program;

	private StringLiteralSet literals;
	private Map<ICClass, DispatchTable> dispatchTables;
	
	private LIRProgram lirprogram;
		
	public LIRTranslationProcess(Program program) {
		this.program = program;
	}
	
	public void run() {
		
		buildGlobalConstants();
		findLastUsed();
	
		translate();
		
	}
	
	private void buildGlobalConstants() {
		GlobalConstantsWrapper wrapper = (GlobalConstantsWrapper)
				program.accept(new BuildGlobalConstants());
		this.literals = wrapper.getLiterals();
		this.dispatchTables = wrapper.getDispatchTables();
	}
	
	private void findLastUsed() {
		program.accept(new SymbolLastUsed());
	}
	
	private void translate() {
		lirprogram = (LIRProgram)program.accept(
				new TranslateIC2LIR(literals, dispatchTables));
	}
	
	public void saveToFile(String filename) {
		
		String lir = (String)lirprogram.accept(new LIRPrinter());
		
		try {
			
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(lir);
			bw.close();
			
		} catch (IOException e) {
			System.err.println("failed to create lir file.");
		}
		

	}
}
