package edu.mit.bcs.clevros.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Counter<K> {

    private final Map<K, Double> map;

    public Counter() {
        this.map = new HashMap<>();
    }

    public double get(K key) {
        return map.getOrDefault((K) key, 0.0);
    }

    public double addTo(K key, double delta) {
        double newVal = map.compute(key, (k, v) -> v == null ? delta : v + delta);
        return newVal;
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Set<Map.Entry<K, Double>> entrySet() {
        return map.entrySet();
    }

    public void normalize() {
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0.0)
            return;

        for (K key : map.keySet())
            map.compute(key, (k, v) -> v / sum);
    }
}
