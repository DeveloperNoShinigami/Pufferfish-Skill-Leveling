package net.bluelotuscoding.puffishskillleveling.expression;

public record GroupOperator(String openToken, String closeToken) {

	public static GroupOperator create(String openToken, String closeToken) {
		return new GroupOperator(openToken, closeToken);
	}

}
