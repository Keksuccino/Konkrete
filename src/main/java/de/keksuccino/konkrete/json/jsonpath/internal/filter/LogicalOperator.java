package de.keksuccino.konkrete.json.jsonpath.internal.filter;

import de.keksuccino.konkrete.json.jsonpath.InvalidPathException;

public enum LogicalOperator {

    AND("&&"),
    NOT("!"),
    OR("||");

    private final String operatorString;

    LogicalOperator(String operatorString) {
        this.operatorString = operatorString;
    }

    public String getOperatorString() {
        return operatorString;
    }

    @Override
    public String toString() {
        return operatorString;
    }

    public static LogicalOperator fromString(String operatorString){
        if(AND.operatorString.equals(operatorString)) return AND;
        else if(NOT.operatorString.equals(operatorString)) return NOT;
        else if(OR.operatorString.equals(operatorString)) return OR;
        else throw new InvalidPathException("Failed to parse operator " + operatorString);
    }
}
