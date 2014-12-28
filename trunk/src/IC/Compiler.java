package IC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import java_cup.runtime.Symbol;
import IC.AST.ICClass;
import IC.AST.PrettyPrinter;
import IC.AST.Program;
import IC.Parser.Lexer;
import IC.Parser.LexicalError;
import IC.Parser.LibParser;
import IC.Parser.Parser;
import IC.Parser.SyntaxError;
import IC.Semantics.ScopesTypesPrinter;
import IC.Semantics.SemanticChecks;
import IC.Semantics.Exceptions.SemanticError;
import LIR.LIRTranslationProcess;

/**
* @team Ori_Idan_Guy
*  1. 200440694
*  2. 200789691
*  3. 300455672
*/
public class Compiler {
	
    public static void main(String[] args) {
    	
    	/* check validity of arguments: */
    	
    	if (args.length < 1 || args.length > 4) {
    		if (args.length == 0)
    			System.err.println("Input filename is missing from arguments.");
    		else
    			System.err.println("Too many arguments supplied.");
    		return;
    	}
    	
    	boolean printAST = false;
    	boolean dumpSymtab = false;
    	
    	String icFileName = args[0];
    	String libFileName = null;
    	
    	for (int i = 1; i < args.length; i++) {
    		
    		String arg = args[i];
    		
    		if (arg.startsWith("-L")) {
    			
    			if (libFileName != null) {
    				System.err.println("Error: can only specify one library file!");
    				return;
    			}
    			
    			libFileName = arg.substring(2);
    			
    		} else if (arg.equals("-print-ast")) {
    			
    			if (printAST) {
    				System.err.println("Error: '-print-ast' option is specified more than once.");
    				return;
    			}
    			
    			printAST = true;
    			
    		} else if (arg.equals("-dump-symtab")) {
    			
    			if (dumpSymtab) {
    				System.err.println("Error: '-dump-symtab' option is specified more than once.");
    				return;
    			}
    			
    			dumpSymtab = true;
    		} else {
    			System.err.println("Error: Unknown option '" + arg + "'.");
    			return;
    		}
    	}
    	    	
    	//extract files and check if exists :
    	
    	File icFile = new File(icFileName);
    	File libFile = libFileName == null ? null : new File(libFileName);
    	
    	if (!icFile.exists()) {
    		System.err.println("File not found: " + icFile);
    		return;
    	}

    	if (libFile != null && !libFile.exists()) {
    		System.err.println("File not found: " + libFile);
    		return;
    	}
    	
    	/* arguments are all right. proceed to running lexer and parser: */
    	
    	try {

    		ICClass libraryClass = null;
    		
    		//parse the library file:
    		if (libFile != null) {
    			Lexer libLexer = null;
    			try {
    				
		    		libLexer = new Lexer(new FileReader(libFile));
		    		LibParser lp = new LibParser(libLexer);
		    		Symbol result = lp.parse();
		    		
		    		if (lp.getSyntaxError() != null) {
		    			System.err.println("Error parsing Library file:");
		    			printError(lp.getSyntaxError());
		    			return;
		    		}
		    		
	    			System.out.println("Parsed " + libFile + " successfully!");
		    		libraryClass = ((Program)result.value).getClasses().get(0);
		    		
	    		} catch (SyntaxError e) {
	    			
	    			System.err.println("Error parsing Library file:");
	    			printError(e);
	    			return;
	    			
	    		} catch (LexicalError e) {
	    			
	    			//lexical error - notify programmer of error and its' location as best as you can:
	    			System.err.println("Lexical error for Library file: ");
	    			printError(new SyntaxError(e.getMessage(), "lexical error", libLexer.getLineNumber(), libLexer.getColumnNumber()));
	    			return;
	    			
	    		}
    		}
    		
    		//parse the IC file:
    		Lexer lexer = null;
    		try {

    			lexer = new Lexer(new FileReader(icFile));    		    		
	    		Parser p = new Parser(lexer);
	    		Symbol result = p.parse();
	    		
	    		//check if any error occurred. If so, notify programmer of all of them:
	    		List<SyntaxError> errors = p.getSyntaxErrors();
	    		if (errors.size() > 0) {
	    			if (libFile != null)
	    				System.err.println("Error parsing IC file:");
	    			for (SyntaxError error : errors)
	    				printError(error);
	    		} else {
	    			
	    			//no error occurred, parse was successful :)

	    			System.out.println("Parsed " + args[0] + " successfully!");
	    			
	    			if (libraryClass != null)
	    				((Program)result.value).getClasses().add(0, libraryClass);
	    			
	    			SemanticChecks semantics = new SemanticChecks(
	    					((Program)result.value),
	    					args[0], libraryClass != null);

    				semantics.run();
    				
    				new LIRTranslationProcess((Program)result.value).run();
    				
	    			if (printAST) {
	    				System.out.println(((Program)result.value).accept(new PrettyPrinter(args[0])));
	    			}
	    				
	    			if (dumpSymtab) {
		    			System.out.println("");
	    				new ScopesTypesPrinter(semantics.getGlobalScope(), semantics.getTypeTable()).print();	    				
	    			}
	    		}
	    		
    		} catch (SyntaxError e) {
    			
    			//usually means it's a fatal SyntaxError (not a "token not expected" kind):
    			if (libFile != null)
    				System.err.println("Error parsing IC file:");
    			printError(e);
    			
    		} catch (LexicalError e) {
    			
    			//lexical error - notify programmer of error and its' location as best as you can:
    			if (libFile != null)
    				System.err.println("Lexical error for IC file:");
    			printError(new SyntaxError(e.getMessage(), "lexical error", lexer.getLineNumber(), lexer.getColumnNumber()));
    			return;
    			
    		} catch (SemanticError e) {
    			System.err.println("semantic error at line " + e.getLine() + ": " + e.getMessage());
    			return;
    		}
    		
    		    		
		} catch (FileNotFoundException e) {
			//should not happen (already checked above!)
			//do nothing
		} catch (Exception e) {
			System.err.println("Unknown error occurred.");
			e.printStackTrace();
		}
    	
    }
        
    private static void printError(SyntaxError e) {
    	System.err.println(e.getLine() + ":" + e.getColumn() + " : " + e.getType() + "; " + e.getMessage());
    }

}
