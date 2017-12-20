package edu.cornell.cs.nlp.spf.data.situated;

import static edu.cornell.cs.nlp.spf.data.situated.CLEVRTypes.*;

public class CLEVRObject {
    private final CLEVRColor color;
    private final CLEVRSize size;
    private final CLEVRShape shape;
    private final CLEVRMaterial material;

    private final double rotation;
    private final double x;
    private final double y;
    private final double z;

    public CLEVRObject(CLEVRColor color, CLEVRSize size, CLEVRShape shape, CLEVRMaterial material,
                       double rotation, double x, double y, double z) {
        this.color = color;
        this.size = size;
        this.shape = shape;
        this.material = material;
        this.rotation = rotation;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
