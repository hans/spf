package edu.cornell.cs.nlp.spf.data.situated.labeled;

import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.Evaluation;
import edu.cornell.cs.nlp.spf.mr.lambda.exec.naive.LambdaResult;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.*;

public class CLEVRScene {

    private final int imageIndex;
    private final Set<CLEVRObject> objects;
    private final Map<CLEVRRelation, List<List<CLEVRObject>>> relations;

    public CLEVRScene(int imageIndex, Set<CLEVRObject> objects, Map<CLEVRRelation, List<List<CLEVRObject>>> relations) {
        this.imageIndex = imageIndex;
        this.objects = objects;
        this.relations = relations;
    }

    public static CLEVRScene buildFromJSON(JSONObject scene) {
        List<CLEVRObject> objects = new ArrayList<>();
        Map<CLEVRRelation, List<List<CLEVRObject>>> relations = new HashMap<>();

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
            List<List<CLEVRObject>> allInstances = new ArrayList<>();

            for (int i = 0; i < allIdxs.size(); i++) {
                JSONArray idxs = (JSONArray) allIdxs.get(i);
                List<CLEVRObject> objRelations = new ArrayList<>();
                for (Object idx : idxs) {
                    objRelations.add(objects.get((int) idx));
                }

                allInstances.add(objRelations);
            }

            relations.put(relation, allInstances);
        }

        // Order of objects collection doesn't matter now.
        Set<CLEVRObject> objectsSet = new HashSet<>(objects);

        return new CLEVRScene(
                (int) scene.get("image_index"),
                objectsSet,
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

    private Object evaluate(String exprString) {
        LogicalExpression expr = Simplify.of(LogicalExpression.read(exprString));
        return evaluate(expr);
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public Set<CLEVRObject> getObjects() {
        return objects;
    }

    public Map<CLEVRRelation, List<List<CLEVRObject>>> getRelations() {
        return relations;
    }


    public static void main(String[] args) {
        final File typesFile = new File("clevr_basic/resources/geo.types");

        List<File> ontFiles = new ArrayList<>();
        ontFiles.add(new File("clevr_basic/resources/geo.preds.ont"));
        ontFiles.add(new File("clevr_basic/resources/geo.consts.ont"));

        try {
            LogicLanguageServices.setInstance(
                    new LogicLanguageServices.Builder(
                            new TypeRepository(typesFile),
                            new FlexibleTypeComparator())
                            .setNumeralTypeName("i").setUseOntology(true)
                            .addConstantsToOntology(ontFiles).closeOntology(true)
                            .build()
            );
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        Set<CLEVRObject> objects = new HashSet<>();
        objects.add(new CLEVRObject("black", "large", "cylinder", "metal",
                0, 0, 0, 0));
        objects.add(new CLEVRObject("green", "small", "cylinder", "metal",
                0, 0, 0, 0));

        CLEVRScene scene = new CLEVRScene(0, objects, null);

        System.out.println(scene.evaluate(
                "(query_material:<e,pm> (unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi)))"
        ));

        System.out.println(scene.evaluate(
                "(same_shape:<e,<e,t>> " +
                        "(unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))" +
                        "(unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> small:psi)))"
        ));

        System.out.println(scene.evaluate(
                "(union:<<e,t>,<<e,t>,<e,t>>> " +
                        "(filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi)" +
                        "(filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> small:psi))"
        ));
    }

}
