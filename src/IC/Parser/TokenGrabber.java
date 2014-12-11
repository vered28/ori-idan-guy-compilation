package IC.Parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

public class TokenGrabber {

	private LexicalError error = null;
	
	public Collection<Token> grab(Reader reader) {
		
		error = null;
		
		ArrayList<Token> tokens = new ArrayList<Token>();
		Lexer lexer =  new Lexer(reader);
		
		try {
			
			Token t = null;
			while ((t = lexer.next_token()) != null && t.getID() != sym.EOF) {
				tokens.add(t);
			}
			
		} catch (IOException e) {
			error = new LexicalError("Error reading input file.");
		} catch (LexicalError e) {
			error = e;
 		} finally {
 			try {
 				reader.close();
 			} catch (IOException e) {
 				//do nothings
 			}
 		}
		
		return tokens;
		
	}
	
	public LexicalError getLexicalError() {
		return error;
	}
	
}
