package IC;

/**
 * Enum of the IC language's binary operators.
 * 
 * @author Tovi Almozlino
 */
public enum BinaryOps {

	PLUS("+", BinaryOpType.MATH, "addition"),
	MINUS("-", BinaryOpType.MATH, "subtraction"),
	MULTIPLY("*", BinaryOpType.MATH, "multiplication"),
	DIVIDE("/", BinaryOpType.MATH, "division"),
	MOD("%", BinaryOpType.MATH, "modulo"),
	LAND("&&", BinaryOpType.LOGICAL, "logical and"),
	LOR("||", BinaryOpType.LOGICAL, "logical or"),
	LT("<", BinaryOpType.SIZE_COMPARISON, "less than"),
	LTE("<=", BinaryOpType.SIZE_COMPARISON, "less than or equal to"),
	GT(">", BinaryOpType.SIZE_COMPARISON, "greater than"),
	GTE(">=", BinaryOpType.SIZE_COMPARISON, "greater than or equal to"),
	EQUAL("==", BinaryOpType.EQUALITY, "equality"),
	NEQUAL("!=", BinaryOpType.EQUALITY, "inequality");
	
	private String operator;
	private BinaryOpType operationType;
	private String description;

	private BinaryOps(String operator, BinaryOpType operationType,
			String description) {
		this.operator = operator;
		this.operationType = operationType;
		this.description = description;
	}

	/**
	 * Returns a string representation of the operator.
	 * 
	 * @return The string representation.
	 */
	public String getOperatorString() {
		return operator;
	}
	
	/**
	 * Returns a description of the operator.
	 * 
	 * @return The description.
	 */
	public String getDescription() {
		return description;
	}
	
	public boolean isMathOperation() {
		return operationType == BinaryOpType.MATH;
	}
	
	public boolean isConcatenationOperation() {
		return operationType == BinaryOpType.CONCATENATION;
	}
	
	public boolean isEqualityOperation() {
		return operationType == BinaryOpType.EQUALITY;
	}
	
	public boolean isSizeComparisonOperation() {
		return operationType == BinaryOpType.SIZE_COMPARISON;
	}
	
	public boolean isLogicalOperation() {
		return operationType == BinaryOpType.LOGICAL;
	}
	
	private enum BinaryOpType {
		MATH,
		CONCATENATION,
		EQUALITY,
		SIZE_COMPARISON,
		LOGICAL;
	}
}