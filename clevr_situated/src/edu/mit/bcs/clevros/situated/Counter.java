package edu.mit.bcs.clevros.situated;

import java.util.HashMap;
import java.util.Map;

public class Counter<K> {

    private final Map<K, Double> map;

    public Counter() {
        this.map = new HashMap<>();
    }

    public double get(K key) {
        return map.get(key);
    }

    public double addTo(K key, double delta) {
        double newVal = map.compute(key, (k, v) -> v == null ? delta : v + delta);
        return newVal;
    }

    public void normalize() {
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0.0)
            return;

        for (K key : map.keySet())
            map.compute(key, (k, v) -> v / sum);
    }
}
