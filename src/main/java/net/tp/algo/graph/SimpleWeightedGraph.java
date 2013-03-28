package net.tp.algo.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleWeightedGraph<T, W extends Number> implements WeightedGraph<T, W> {

    private Map<T, Map<T, W>> vertices = new HashMap<T, Map<T, W>>();

    private int edgeSize = 0;

    public SimpleWeightedGraph<T, W> v(Collection<T> vertices) {
        for (T v : vertices) {
            v(v);
        }
        return this;
    }

    public SimpleWeightedGraph<T, W> v(T ... vertices) {
        for (T v : vertices) {
            v(v);
        }
        return this;
    }

    public SimpleWeightedGraph<T, W> v(T vertex) {

        if (!vertices.containsKey(vertex)) {
            vertices.put(vertex, new HashMap<T, W>());
        }
        return this;
    }


    public SimpleWeightedGraph<T, W> e(T v1, T v2, W weight) {

        if (v1.equals(v2)) {
            throw new IllegalArgumentException();
        }

        v(v1);
        Map<T, W> edges1 = vertices.get(v1);

        v(v2);
        Map<T, W> edges2 = vertices.get(v2);

        edges1.put(v2, weight);
        edges2.put(v1, weight);
        edgeSize++;

        return this;
    }

    /**
     * number of edges
     * @return
     */
    @Override
    public int m() {
        return edgeSize;
    }

    /**
     * number of vertices
     * @return
     */
    @Override
    public int n() {
        return vertices.size();
    }

    @Override
    public Set<T> vertices() {
        return this.vertices.keySet();
    }

    @Override
    public Set<T> edges(T vertex) {
        Map<T, W> e = this.vertices.get(vertex);
        return e.keySet();
    }

    @Override
    public W weight(T u, T w) {
        Map<T, W> e = this.vertices.get(u);
        return e == null ? null : e.get(w);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        Map<T, Set<T>> dupEdges = new HashMap<T, Set<T>>();

        for (T u : vertices()) {
            for (T w : edges(u)) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }

                if (dupEdges.containsKey(u) && dupEdges.get(u).contains(w)) {
                    continue;
                }
                if (!dupEdges.containsKey(w)) {
                    dupEdges.put(w, new HashSet<T>());
                }
                dupEdges.get(w).add(u);

                builder.append(u).append('-').append(w);
            }
        }

        return builder.toString();
    }

}