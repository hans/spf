package edu.cornell.cs.nlp.spf.data.situated;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

import static edu.cornell.cs.nlp.spf.data.situated.CLEVRTypes.*;

public class CLEVRScene {

    private final int imageIndex;
    private final List<CLEVRObject> objects;
    private final Map<CLEVRRelation, List<List<CLEVRObject>>> relations;

    public CLEVRScene(int imageIndex, List<CLEVRObject> objects, Map<CLEVRRelation, List<List<CLEVRObject>>> relations) {
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
                    CLEVRColor.valueOf(((String) obj_.get("color")).toUpperCase()),
                    CLEVRSize.valueOf(((String) obj_.get("size")).toUpperCase()),
                    CLEVRShape.valueOf(((String) obj_.get("shape")).toUpperCase()),
                    CLEVRMaterial.valueOf(((String) obj_.get("material")).toUpperCase()),
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

        return new CLEVRScene(
                (int) scene.get("image_index"),
                objects,
                relations
        );
    }

}
