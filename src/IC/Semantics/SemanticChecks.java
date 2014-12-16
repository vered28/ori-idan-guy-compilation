package IC.Semantics;

import IC.AST.Program;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Scopes.ScopesBuilder;
import IC.Semantics.Validations.ControlStatementsValidation;
import IC.Semantics.Validations.DeclarationValidation;
import IC.Semantics.Validations.NonCircularScopesValidation;

public class SemanticChecks {

	private Program program;
	private ProgramScope mainScope;
	
	private String filename;
	private boolean hasLibrary;
	
	public SemanticChecks(Program program, String filename, boolean hasLibrary) {
		this.program = program;
		this.filename = filename;
		this.hasLibrary = hasLibrary;
	}
	
	public void run() {
		
		//build symbol tables (scope instance for each scope):
		mainScope = buildScopes();
		
		//check that scopes graph is not circular:
		validateNotCircular();
		
		/* *********** perform semantic checks: ************/
		
		//1 - validate scope rules:
		validateDeclarations();
		
		//TODO: 2 - type checking
		
		//TODO: 3 - single main check (can be done only after type checking)
		
		//4+5 - validate control statements (break / continue / this):
		validateControlStatements();
		
	}
	
	public ProgramScope getMainScope() {
		return mainScope;
	}
	
	private ProgramScope buildScopes() {
		return (ProgramScope)program.accept(
				new ScopesBuilder(filename, hasLibrary));
	}
	
	private void validateNotCircular() {
		mainScope.accept(new NonCircularScopesValidation());
	}
	
	private void validateDeclarations() {
		program.accept(new DeclarationValidation());
	}
	
	private void validateControlStatements() {
		program.accept(new ControlStatementsValidation());
	}
	
}
