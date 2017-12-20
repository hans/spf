package edu.cornell.cs.nlp.spf.data.situated.labeled;

import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.*;

public class CLEVRObject {
    private final String color;
    private final String size;
    private final String shape;
    private final String material;

    private final double rotation;
    private final double x;
    private final double y;
    private final double z;

    public CLEVRObject(String color, String size, String shape, String material,
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

//    public <T extends Enum<T>> T getAttribute(Class<T> attribute) {
//        if (attribute == CLEVRColor.class)
//            return (T) this.color;
//        else if (attribute == CLEVRSize.class)
//            return (T) this.size;
//        else if (attribute == CLEVRShape.class)
//            return (T) this.shape;
//        else if (attribute == CLEVRMaterial.class)
//            return (T) this.material;
//        else
//            throw new RuntimeException("unknown attribute class " + attribute.toString());
//    }

    public String getAttribute(String attribute) {
        switch (attribute) {
            case "color":
                return getColor();
            case "size":
                return getSize();
            case "shape":
                return getShape();
            case "material":
                return getMaterial();
        }

        return null;
    }

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public String getShape() {
        return shape;
    }

    public String getMaterial() {
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
