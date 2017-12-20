package edu.cornell.cs.nlp.spf.data.situated.labeled;

import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.*;

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

    public <T extends Enum<T>> T getAttribute(Class<T> attribute) {
        if (attribute == CLEVRColor.class)
            return (T) this.color;
        else if (attribute == CLEVRSize.class)
            return (T) this.size;
        else if (attribute == CLEVRShape.class)
            return (T) this.shape;
        else if (attribute == CLEVRMaterial.class)
            return (T) this.material;
        else
            throw new RuntimeException("unknown attribute class " + attribute.toString());
    }

    public CLEVRColor getColor() {
        return color;
    }

    public CLEVRSize getSize() {
        return size;
    }

    public CLEVRShape getShape() {
        return shape;
    }

    public CLEVRMaterial getMaterial() {
        return material;
    }

    public double getRotation() {
        return rotation;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}