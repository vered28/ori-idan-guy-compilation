package IC.Semantics;

import IC.AST.Program;
import IC.Semantics.Exceptions.SemanticError;
import IC.Semantics.Scopes.ProgramScope;
import IC.Semantics.Types.TypeTable;
import IC.Semantics.Validations.ControlStatementsValidation;
import IC.Semantics.Validations.DeclarationValidation;
import IC.Semantics.Validations.LocalVariableInitializedValidation;
import IC.Semantics.Validations.NonCircularScopesValidation;
import IC.Semantics.Validations.NonVoidAlwaysReturnsValidation;
import IC.Semantics.Validations.SingleMainValidation;
import IC.Semantics.Validations.TypesValidation;

public class SemanticChecks {

	private Program program;
	private ProgramScope globalScope;
	private TypeTable typeTable;
	
	private String filename;
	private boolean hasLibrary;
	
	public SemanticChecks(Program program, String filename, boolean hasLibrary) {
		this.program = program;
		this.filename = filename;
		this.hasLibrary = hasLibrary;
	}
	
	public void run() {
		
		//build symbol (scope instance for each scope) and type tables :
		buildScopesAndTypes();
		
		//check that scopes graph is not circular:
		validateNotCircular();
		
		/* *********** perform semantic checks: ************/
		
		//1 - validate scope rules:
		validateDeclarations();
		
		// 2 - type checking
		validateTypes();
		
		//3 - single main check (can be done only after type checking)
		validateSingleMain();
		
		//4+5 - validate control statements (break / continue / this):
		validateControlStatements();
		
		/* *********** bonus checks: ************/
		
		try {
			
			//1 - a local variable is used only after it has been initialized
			validateLocalVariableInitialized();
			
			//2 - a method with a non-void return type returns a value on every control path 
			validateNonVoidAlwaysReturns();
			
		} catch (SemanticError e) {
			throw e;
		} catch (Exception e) {
			System.err.println("Exception during bonus checks...");
		}
		
	}
	
	public ProgramScope getGlobalScope() {
		return globalScope;
	}
	
	public TypeTable getTypeTable() {
		return typeTable;
	}
	
	private void buildScopesAndTypes() {
		ScopesTypesWrapper wrapper = (ScopesTypesWrapper)program.accept(
				new ScopesTypesBuilder(filename, hasLibrary));
		typeTable = wrapper.getTypeTable();
		globalScope = wrapper.getGlobalScope();
	}
	
	private void validateNotCircular() {
		globalScope.accept(new NonCircularScopesValidation());
	}
	
	private void validateDeclarations() {
		program.accept(new DeclarationValidation());
	}
	
	private void validateControlStatements() {
		program.accept(new ControlStatementsValidation());
	}
	
	private void validateTypes() {
		program.accept(new TypesValidation(typeTable));
	}
	
	private void validateSingleMain() {
		program.accept(new SingleMainValidation());
	}
	
	private void validateLocalVariableInitialized() {
		program.accept(new LocalVariableInitializedValidation());
	}
	
	private void validateNonVoidAlwaysReturns() {
		program.accept(new NonVoidAlwaysReturnsValidation());
	}
	
}
