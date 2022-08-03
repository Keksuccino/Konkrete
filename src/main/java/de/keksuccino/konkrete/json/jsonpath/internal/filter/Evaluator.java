package de.keksuccino.konkrete.json.jsonpath.internal.filter;

import de.keksuccino.konkrete.json.jsonpath.Predicate;

public interface Evaluator {
    boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx);
}