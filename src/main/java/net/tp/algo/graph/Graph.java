package net.tp.algo.graph;

import java.util.Set;

public interface Graph<T> {
	
	public int n();
	
	public int m();

	public Set<T> vertices();
	
	public Set<T> edges(T from);

}
