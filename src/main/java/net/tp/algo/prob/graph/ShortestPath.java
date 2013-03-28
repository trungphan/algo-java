package net.tp.algo.prob.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import net.tp.algo.graph.SimpleWeightedGraph;
import net.tp.algo.graph.WeightedGraph;
import net.tp.algo.tree.BinaryHeap;
import net.tp.algo.tree.Heap;
import net.tp.algo.tree.HeapFactory;
import net.tp.algo.util.Ref;

import org.junit.Assert;
import org.junit.Test;

import com.sun.org.apache.xalan.internal.xsltc.dom.MultiValuedNodeHeapIterator.HeapNode;

public class ShortestPath {

	
	
	
	/**
	 * 
	 * Dijkstra's algorithm is greedy algorithm for weighted graph with positive weight.
	 * If the weight is not positive, the algorithm is guaranteed to terminate, but not
	 * guaranteed to give shortest path.
	 * 
	 * Running time: for binary heap: O ((|V|+|E|)log(|V|)) = O(|E|log(|V|)).
	 * For Fibonacci heap: O(|E| + |V|log(|V|)) time.
	 * 
	 * For non-weight graph, use bread first search algorithm.
	 * For negative weight graph, use Bellman-Ford algorithm.
	 * 
	 * @param graph
	 * @param source
	 * @param heapFactory provide customized implementation for better performance
	 */
	public static <T> Map<T, Integer> dijkstra(WeightedGraph<T, Integer> graph, T source, HeapFactory<T> heapFactory) {

		final Map<T, Integer> distances = new HashMap<>(); // store the shortest distances between node u to source.
		distances.put(source, 0);
		
		Set<T> settledSet = new HashSet<>();
		
		// priority queue to prioritize the shortest distance from source to nodes in unsettled set
		Heap<T> queue = heapFactory.makeHeap(new Comparator<T>() {
			@Override
			public int compare(T v1, T v2) {
				return distances.get(v1).compareTo(distances.get(v2));
			}
		});
		
		Map<T, Ref> queueElementRef = new HashMap<>(); // to store the ref to element in queue for quick operations.
		queue.add(source);
		
		while (!queue.empty()) {
			T u = queue.removeHead();
			settledSet.add(u);
			
			for (T v : graph.edges(u)) {
				if (settledSet.contains(v)) {
					continue;
				}
				
				Integer currDistance = distances.get(v); // if currDistance == null, it's assumed to be Integer.MAX_VALUE
				if (currDistance == null || currDistance > distances.get(u) + graph.weight(u, v)) {
					distances.put(v, distances.get(u) + graph.weight(u, v));
					
					Ref ref = queueElementRef.get(v);
					if (ref == null || !ref.valid()) {
						ref = queue.add(v);
						queueElementRef.put(v, ref);
					}
					else {
						queue.update(ref);
					}
				}
			}
		}
		
		return distances;
	}
	
	
	public static class TestCase {
		
        @Test
        public void shortest_path() {
        	SimpleWeightedGraph<Integer, Integer> graph = new SimpleWeightedGraph<>();
        	graph.v(1, 2, 3, 4, 5)
        		.e(1, 2, 5)
        		.e(1, 3, 3)
        		.e(2, 3, 2);

        	Map<Integer, Integer> distances = ShortestPath.dijkstra(graph, 1, HeapFactory.binaryHeap(Integer.class));

            System.out.println(distances);
        }

        @Test
        public void shortest_path_dijkstra() throws IOException {
            SimpleWeightedGraph<Integer, Integer> graph = new SimpleWeightedGraph<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("dijkstraData.txt")));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll(",", " ");
                try (Scanner scanner = new Scanner(line)) {
                    int u = scanner.nextInt();
                    graph.v(u);
                    while (scanner.hasNext()) {
                        int w = scanner.nextInt();
                        int weight = scanner.nextInt();
                        graph.e(u, w, weight);
                    }
                }
            }


            long startTime = System.nanoTime();
            Map<Integer, Integer> distances = ShortestPath.dijkstra(graph, 1, HeapFactory.fibonacciHeap(Integer.class));
            long duration = System.nanoTime() - startTime;
            System.out.println("Duration: " + duration / 1000000);

            List<Integer> nodes = Arrays.asList(7,37,59,82,99,115,133,165,188,197);
            List<Integer> expectedShortestPaths = Arrays.asList(2599,2610,2947,2052,2367,2399,2029,2442,2505,3068);
            for (int i = 0; i < nodes.size(); i++) {
            	Assert.assertEquals(expectedShortestPaths.get(i), distances.get(nodes.get(i)));
            }
            
        }
	}
	
	
}
