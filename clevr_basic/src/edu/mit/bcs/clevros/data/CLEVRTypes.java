package edu.mit.bcs.clevros.data;

import java.util.HashMap;
import java.util.Map;

public class CLEVRTypes {

    public static final String[] SHAPES = new String[] { "sphere", "cylinder", "cube" };
    public static final String[] SIZES = new String[] { "small", "large" };
    public static final String[] COLORS = new String[] { "green", "purple", "blue", "brown", "gray", "cyan", "yellow", "red" };
    public static final String[] MATERIALS = new String[] { "metal", "rubber" };

    public static final Map<String, String[]> PROPERTIES = new HashMap<>();
    static {
        PROPERTIES.put("shape", SHAPES);
        PROPERTIES.put("size", SIZES);
        PROPERTIES.put("color", COLORS);
        PROPERTIES.put("material", MATERIALS);
    }

    public enum CLEVRRelation {
        RIGHT, BEHIND, FRONT, LEFT
    }
}
