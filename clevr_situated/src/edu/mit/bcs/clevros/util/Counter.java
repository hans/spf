package edu.mit.bcs.clevros.util;

import java.util.*;
import java.util.stream.Collectors;

public class Counter<K> {

    private final double defaultValue;
    private final Map<K, Double> map;

    public Counter() {
        this(0.0);
    }

    public Counter(double defaultValue) {
        this.defaultValue = defaultValue;
        this.map = new HashMap<>();
    }

    public Counter(double defaultValue, List<K> initialKeys) {
        this(defaultValue);

        for (K key : initialKeys)
            get(key);
    }

    public K argmax() {
        return map.keySet().stream().max((k1, k2) -> Double.compare(map.get(k2), map.get(k1))).orElse(null);
    }

    public double get(K key) {
        return map.computeIfAbsent(key, k -> defaultValue);
    }

    public void put(K key, double value) {
        map.put(key, value);
    }

    public double addTo(K key, double delta) {
        return map.compute(key, (k, v) -> v == null
                ? defaultValue + delta : v + delta);
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Set<Map.Entry<K, Double>> entrySet() {
        return map.entrySet();
    }

    public void normalize() {
        normalize(1.0);
    }

    public void normalize(double toSum) {
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum == 0.0)
            return;

        for (K key : map.keySet())
            map.compute(key, (k, v) -> v / sum * toSum);
    }

    @Override
    public String toString() {
        String sortedEntriesStr = entrySet()
                .stream().sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(entry -> String.format("%s=>%.3f", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","));
        return "Counter{" + sortedEntriesStr + "}";
    }
}
