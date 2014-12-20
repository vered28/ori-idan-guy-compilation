package IC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import java_cup.runtime.Symbol;
import IC.AST.ICClass;
import IC.AST.Program;
import IC.Parser.Lexer;
import IC.Parser.LexicalError;
import IC.Parser.LibParser;
import IC.Parser.Parser;
import IC.Parser.SyntaxError;
import IC.Semantics.SemanticChecks;
import IC.Semantics.SemanticError;
import IC.Semantics.Scopes.ScopesPrinter;

/**
* @team Ori_Idan_Guy
*  1. 200440694
*  2. 200789691
*  3. 300455672
*/
public class Compiler {
	
    public static void main(String[] args) {
    	
    	/* check validity of arguments: */
    	
    	if (args.length < 1 || args.length > 2) {
    		if (args.length == 0)
    			System.err.println("Input filename is missing from arguments.");
    		else
    			System.err.println("Too many arguments supplied.");
    		return;
    	}
    	
    	//if library is supplied, it must be supplied as -Lfilename :
    	if (args.length > 1 && !args[1].startsWith("-L")) {
    		System.err.println("Library argument is illegal. Must be captioned with a -L prefix.");
    		return;
    	}
    	
    	//extract files and check if exists :
    	
    	String icFileName = args[0];
    	String libFileName = (args.length > 1 ? args[1].substring(2) : null);
    	
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
		    		
	    			//System.out.println("Parsed " + libFile + " successfully!");
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
	    			
	    			if (libraryClass != null)
	    				((Program)result.value).getClasses().add(0, libraryClass);
	    			
	    			SemanticChecks semantics = new SemanticChecks(
	    					((Program)result.value),
	    					args[0], libraryClass != null);
	    			try {
	    				semantics.run();	    					
	    			} catch (SemanticError e) {
	    				System.err.println(e.getMessage());
	    				//e.printStackTrace();
	    			}
	    			
	    			//System.out.println("Parsed " + args[0] + " successfully!");
	    			//System.out.println(new PrettyPrinter(args[0]).visit((Program) result.value));
	    			System.out.println(semantics.getMainScope().accept(new ScopesPrinter()));
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
