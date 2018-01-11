package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.Evaluation;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.LambdaResult;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.mit.bcs.clevros.situated.CLEVREvaluationServices;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

import static edu.mit.bcs.clevros.data.CLEVRTypes.CLEVRRelation;

public class CLEVRScene {

    private final int imageIndex;
    private final List<CLEVRObject> objects;
    private final Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> relations;

    public CLEVRScene(int imageIndex, List<CLEVRObject> objects,
                      Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> relations) {
        this.imageIndex = imageIndex;
        this.objects = objects;
        this.relations = relations;
    }

    public static CLEVRScene buildFromJSON(JSONObject scene) {
        List<CLEVRObject> objects = new ArrayList<>();
        Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> relations = new HashMap<>();

        for (Object obj : (JSONArray) scene.get("objects")) {
            JSONObject obj_ = (JSONObject) obj;

            JSONArray coords = (JSONArray) obj_.get("3d_coords");
            objects.add(new CLEVRObject(
                    (String) obj_.get("color"),
                    (String) obj_.get("size"),
                    (String) obj_.get("shape"),
                    (String) obj_.get("material"),
                    (double) obj_.get("rotation"),
                    (double) coords.get(0), (double) coords.get(1),
                    (double) coords.get(2)));
        }

        JSONObject relationships = (JSONObject) scene.get("relationships");
        for (CLEVRRelation relation : CLEVRRelation.values()) {
            JSONArray allIdxs = (JSONArray) relationships.get(relation.toString().toLowerCase());
            Map<CLEVRObject, Set<CLEVRObject>> allInstances = new HashMap<>();

            for (int i = 0; i < allIdxs.size(); i++) {
                JSONArray idxs = (JSONArray) allIdxs.get(i);
                Set<CLEVRObject> objRelations = new HashSet<>();
                for (Object idx : idxs) {
                    objRelations.add(objects.get(((Long) idx).intValue()));
                }

                allInstances.put(objects.get(i), objRelations);
            }

            relations.put(relation, allInstances);
        }

        return new CLEVRScene(
                ((Long) scene.get("image_index")).intValue(),
                objects,
                relations
        );
    }

    public CLEVRAnswer evaluate(LogicalExpression expr) {
        CLEVREvaluationServices services = new CLEVREvaluationServices(this);
        Object ret = Evaluation.of(expr, services);

        if (ret instanceof LambdaResult) {
            LambdaResult matches = (LambdaResult) ret;
            final HashSet<CLEVRObject> retSet = new HashSet<>();
            matches.forEach((tuple) -> retSet.add((CLEVRObject) tuple.get(0)));
            ret = retSet;
        }

        return new CLEVRAnswer(ret);
    }

    public CLEVRAnswer evaluate(String exprString) {
        LogicalExpression expr = Simplify.of(LogicalExpression.read(exprString));
        return evaluate(expr);
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public List<CLEVRObject> getObjects() {
        return objects;
    }

    public Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> getRelations() {
        return relations;
    }

    public boolean hasRelation(CLEVRObject obj1, CLEVRObject obj2, CLEVRRelation relation) {
        if (!objects.contains(obj1) || !objects.contains(obj2))
            throw new RuntimeException("one or both of requested objects are not part of this scene");

        Map<CLEVRObject, Set<CLEVRObject>> relMap = relations.get(relation);
        if (!relMap.containsKey(obj1))
            return false;
        return relMap.get(obj1).contains(obj2);
    }
}
