package edu.cornell.cs.nlp.spf.data.situated.labeled;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.AbstractEvaluationServices;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.LambdaResult;
import edu.cornell.cs.nlp.utils.composites.Pair;

import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CLEVREvaluationServices extends AbstractEvaluationServices<CLEVRScene> {

    private final CLEVRScene scene;

    private final Map<String, String> propertyLiterals = new HashMap<>();
    private final Map<String, Function<CLEVRObject, Boolean>> setLiterals = new HashMap<>();
    private final Map<String, CLEVRRelation> relationLiterals = new HashMap<>();

    private final Map<String, Function<LambdaResult, CLEVRObject>> fetchFunctions = new HashMap<>();
    private final Map<String, BiFunction<Pair<LambdaResult, LambdaResult>, CLEVRObject, Boolean>> setOpFunctions = new HashMap<>();
    private final Map<String, BiFunction<CLEVRObject, String, Boolean>> filterFunctions = new HashMap<>();
    private final Map<String, Function<CLEVRObject, String>> queryFunctions = new HashMap<>();
    private final Map<String, BiFunction<CLEVRObject, CLEVRObject, Boolean>> sameFunctions = new HashMap<>();
    private final Map<String, BiFunction<String, String, Boolean>> equalFunctions = new HashMap<>();
    private final Map<String, Function<LambdaResult, Object>> reductionFunctions = new HashMap<>();
    private final Map<String, BiFunction<Integer, Integer, Boolean>> relationFunctions = new HashMap<>();
    private final Map<String, BiFunction<Pair<CLEVRObject, CLEVRRelation>, CLEVRObject, Boolean>> spatialRelationFunctions = new HashMap<>();

    private void seedLiteralsForProperty(String propertyName, String[] vals) {
        for (String val : vals) {
            // define literals like cylinder:psh
            propertyLiterals.put(val.toLowerCase(), val);

            // define literals like cylinder:<e,t>
            setLiterals.put(val.toLowerCase(),
                    (obj) -> obj.getAttribute(propertyName).equals(val));
        }
    }

    public CLEVREvaluationServices(CLEVRScene scene) {
        this.scene = scene;

        // Prepare literal maps.

        CLEVRTypes.PROPERTIES.forEach((name, vals) -> {
            seedLiteralsForProperty(name, vals);

            filterFunctions.put("filter_" + name, (obj, val) -> obj.getAttribute(name).equals(val));
            queryFunctions.put("query_" + name, (obj) -> obj.getAttribute(name));
            sameFunctions.put("same_" + name, (obj1, obj2) ->
                    obj1.getAttribute(name).equals(obj2.getAttribute(name))
            );
            equalFunctions.put("equal_" + name, Object::equals);
        });

        setLiterals.put("scene", (x) -> true);

        fetchFunctions.put("unique", (lresult) -> {
            if (lresult.size() != 1)
                return null;
            return (CLEVRObject) lresult.iterator().next().get(0);
        });

        setOpFunctions.put("union",
                (lrs, obj) -> lrs.first().hasTupleWithKey(obj) || lrs.second().hasTupleWithKey(obj));
        setOpFunctions.put("intersection",
                (lrs, obj) -> lrs.first().hasTupleWithKey(obj) && lrs.second().hasTupleWithKey(obj));

        reductionFunctions.put("exists", (lr) -> lr.size() != 0);
        reductionFunctions.put("count", LambdaResult::size);

        relationFunctions.put("greater_than", (i1, i2) -> i1 > i2);
        relationFunctions.put("less_than", (i1, i2) -> i1 < i2);

        spatialRelationFunctions.put("relate", (pair, candidate) ->
                scene.hasRelation(pair.first(), candidate, pair.second()));
        for (CLEVRRelation reln : CLEVRRelation.values())
            relationLiterals.put(reln.toString().toLowerCase(), reln);
    }

    private void arityCheck(String predicate, int expected, Object[] args) {
        if (args.length != expected)
            throw new RuntimeException("bad arity " + args.length + " for literal " + predicate);
    }

    public Object evaluateLiteral(LogicalExpression predicate, Object[] args) {
        if (!(predicate instanceof LogicalConstant)) {
            throw new RuntimeException("Don't support non-constant predicates such as " + predicate.toString());
        }

        String predicateName = ((LogicalConstant) predicate).getBaseName();

        if (setLiterals.containsKey(predicateName)) {
            //arityCheck(predicateName, 0, args);
            return setLiterals.get(predicateName).apply((CLEVRObject) args[0]);
        } else if (filterFunctions.containsKey(predicateName)) {
            //arityCheck(predicateName, 2, args);
            return filterFunctions.get(predicateName).apply(
                    (CLEVRObject) args[2],
                    (String) args[1]
            );
        } else if (setOpFunctions.containsKey(predicateName)) {
            return setOpFunctions.get(predicateName).apply(
                    Pair.of((LambdaResult) args[0], (LambdaResult) args[1]),
                    (CLEVRObject) args[2]
            );
        } else if (fetchFunctions.containsKey(predicateName)) {
            return fetchFunctions.get(predicateName).apply((LambdaResult) args[0]);
        } else if (queryFunctions.containsKey(predicateName)) {
            return queryFunctions.get(predicateName).apply((CLEVRObject) args[0]);
        } else if (sameFunctions.containsKey(predicateName)) {
            return sameFunctions.get(predicateName).apply((CLEVRObject) args[0], (CLEVRObject) args[1]);
        } else if (equalFunctions.containsKey(predicateName)) {
            return equalFunctions.get(predicateName).apply((String) args[0], (String) args[1]);
        } else if (reductionFunctions.containsKey(predicateName)) {
            return reductionFunctions.get(predicateName).apply((LambdaResult) args[0]);
        } else if (relationFunctions.containsKey(predicateName)) {
            return relationFunctions.get(predicateName).apply((Integer) args[0], (Integer) args[1]);
        } else if (spatialRelationFunctions.containsKey(predicateName)) {
            return spatialRelationFunctions.get(predicateName).apply(
                    Pair.of((CLEVRObject) args[2], (CLEVRRelation) args[1]), (CLEVRObject) args[0]);
        } else {
            throw new RuntimeException("unrecognized literal " + predicateName + " in " + predicate.toString());
        }
    }

    @Override
    public Object evaluateConstant(LogicalConstant logicalConstant) {
        Object ret = super.evaluateConstant(logicalConstant);
        if (ret != null)
            return ret;

        String name = logicalConstant.getBaseName();
        if (propertyLiterals.containsKey(name)) {
            return propertyLiterals.get(name);
        } else if (relationLiterals.containsKey(name)) {
            return relationLiterals.get(name);
        } else {
            throw new RuntimeException("unrecognized constant " + logicalConstant.toString());
        }
    }

    @Override
    public List<?> getAllDenotations(Variable variable) {
        if (!variable.getType().getName().equals("e"))
            throw new RuntimeException("unexpected variable type for variable " + variable.toString());

        return new ArrayList<>(scene.getObjects());
    }

    @Override
    public boolean isDenotable(Variable variable) {
        // TODO what is this? it doesn't matter, because our lxprs never have lambdas at the top level
        return false;
    }

    @Override
    protected CLEVRScene currentState() {
        return scene;
    }

}
