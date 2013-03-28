package net.tp.algo.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleGraph<T> implements Graph<T> {

    private Map<T, Set<T>> vertices;

    private int edgeSize;

    public SimpleGraph() {
        this.edgeSize = 0;
        this.vertices = new HashMap<T, Set<T>>();
    }

    public SimpleGraph<T> v(T ... vertices) {
        for (T v : vertices) {
            v(v);
        }
        return this;
    }

    public SimpleGraph<T> v(T vertex) {
        if (!vertices.containsKey(vertex)) {
            vertices.put(vertex, new HashSet<T>());
        }
        return this;
    }

    public SimpleGraph<T> e(T u, T ... destinations) {
        for (T w : destinations) {
            e(u, w);
        }
        return this;
    }

    public SimpleGraph<T> e(T u, T w) {

        if (u.equals(w)) {
            throw new IllegalArgumentException();
        }

        v(u);
        Set<T> edges1 = vertices.get(u);

        v(w);
        Set<T> edges2 = vertices.get(w);

        if (!edges1.contains(w)) {
            edges1.add(w);
            edges2.add(u);
            edgeSize++;
        }

        return this;
    }

    /**
     * number of edges
     * @return
     */
    public int m() {
        return edgeSize;
    }

    /**
     * number of vertices
     * @return
     */
    public int n() {
        return vertices.size();
    }

    public Set<T> vertices() {
        return this.vertices.keySet();
    }

    public Set<T> edges(T vertex) {
        return this.vertices.get(vertex);
    }


}
