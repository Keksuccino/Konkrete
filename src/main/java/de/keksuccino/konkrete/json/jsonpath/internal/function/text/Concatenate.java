package de.keksuccino.konkrete.json.jsonpath.internal.function.text;

import de.keksuccino.konkrete.json.jsonpath.internal.EvaluationContext;
import de.keksuccino.konkrete.json.jsonpath.internal.PathRef;
import de.keksuccino.konkrete.json.jsonpath.internal.function.Parameter;
import de.keksuccino.konkrete.json.jsonpath.internal.function.PathFunction;

import java.util.List;

/**
 * String function concat - simple takes a list of arguments and/or an array and concatenates them together to form a
 * single string
 *
 */
public class Concatenate implements PathFunction {
    @Override
    public Object invoke(String currentPath, PathRef parent, Object model, EvaluationContext ctx, List<Parameter> parameters) {
        StringBuilder result = new StringBuilder();
        if(ctx.configuration().jsonProvider().isArray(model)){
            Iterable<?> objects = ctx.configuration().jsonProvider().toIterable(model);
            for (Object obj : objects) {
                if (obj instanceof String) {
                    result.append(obj.toString());
                }
            }
        }
        if (parameters != null) {
            for (String value : Parameter.toList(String.class, ctx, parameters)) {
                result.append(value);
            }
        }
        return result.toString();
    }
}
