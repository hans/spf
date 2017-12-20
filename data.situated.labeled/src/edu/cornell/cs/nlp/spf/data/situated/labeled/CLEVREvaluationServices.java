package edu.cornell.cs.nlp.spf.data.situated.labeled;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.AbstractEvaluationServices;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.IEvaluationServices;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;

import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.*;

import java.util.*;
import java.util.function.Function;

public class CLEVREvaluationServices extends AbstractEvaluationServices<CLEVRScene> {

    private final CLEVRScene scene;

    private final Map<String, Enum> propertyLiterals = new HashMap<>();
    private final Map<String, Function<CLEVRScene, Set<CLEVRObject>>> setLiterals = new HashMap<>();

    {
        List<Class<? extends Enum>> propertyEnums = new ArrayList<>();
        propertyEnums.add(CLEVRColor.class);
        propertyEnums.add(CLEVRMaterial.class);
        propertyEnums.add(CLEVRShape.class);
        propertyEnums.add(CLEVRSize.class);

        for (Class<? extends Enum> propertyEnum : propertyEnums) {
            seedLiteralsForProperty(propertyEnum);
        }

        setLiterals.put("scene", CLEVRScene::getObjects);
    }

    private <T extends Enum<T>> void seedLiteralsForProperty(Class<T> propertyClass) {
        for (T val : propertyClass.getEnumConstants()) {
            // define literals like cylinder:psh
            propertyLiterals.put(val.toString().toLowerCase(), val);

            // define literals like cylinder:<e,t>
            Filter<T> filter = new Filter<>(propertyClass, val);
            setLiterals.put(val.toString().toLowerCase(),
                    (scene) -> filter.apply(scene.getObjects()));
        }
    }

    public CLEVREvaluationServices(CLEVRScene scene) {
        this.scene = scene;
    }

    public Object evaluateLiteral(LogicalExpression predicate, Object[] args) {
        if (!(predicate instanceof LogicalConstant)) {
            throw new RuntimeException("Don't support non-constant predicates such as " + predicate.toString());
        }
        String predicateName = ((LogicalConstant) predicate).getBaseName();
        // TODO
        System.out.println("evaluateLiteral" + predicateName);
        System.out.println("\t" + predicate.toString());
        System.out.println(predicate.getClass());

        return args[0];
    }

    @Override
    public Object evaluateConstant(LogicalConstant logicalConstant) {
        System.out.println(logicalConstant);
        Object ret = super.evaluateConstant(logicalConstant);
        if (ret != null)
            return ret;

        // TODO
        return null;
    }

    @Override
    public List<?> getAllDenotations(Variable variable) {
        // TODO
        List<Object> denotations = new ArrayList<>();
        denotations.add(variable.getType().toString());
        return denotations;
    }

    @Override
    public boolean isDenotable(Variable variable) {
        // TODO what is this? it doesn't matter, because our lxprs never have lambdas at the top level
        return true;
    }

    @Override
    protected CLEVRScene currentState() {
        return scene;
    }

    private class Filter<T extends Enum<T>> implements Function<Set<CLEVRObject>, Set<CLEVRObject>> {
        private final Class<T> attribute;
        private final T value;

        public Filter(Class<T> attribute, T value) {
            this.attribute = attribute;
            this.value = value;
        }


        @Override
        public Set<CLEVRObject> apply(Set<CLEVRObject> clevrObjects) {
            Set<CLEVRObject> ret = new HashSet<>();
            for (CLEVRObject obj : clevrObjects) {
                if (obj.getAttribute(attribute).equals(value)) {
                    ret.add(obj);
                }
            }

            return ret;
        }
    }

}
