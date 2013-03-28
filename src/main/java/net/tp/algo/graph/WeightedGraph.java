package net.tp.algo.graph;

public interface WeightedGraph<T, W extends Number> extends Graph<T> {

	public W weight(T u, T w);

}
