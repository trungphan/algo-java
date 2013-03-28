package net.tp.algo.tree;

import java.util.Collection;
import java.util.Comparator;

public abstract class HeapFactory<E> {
	
	public abstract Heap<E> makeHeap(Comparator<? super E> comparator);

	public abstract Heap<E> makeHeap(Comparator<? super E> comparator, Collection<E> elements);

	
	@SuppressWarnings("rawtypes")
	public static BinaryHeapFactory binaryHeap() {
		return new BinaryHeapFactory<>();
	}
	
	public static <E> BinaryHeapFactory<E> binaryHeap(Class<E> c) {
		return new BinaryHeapFactory<>();
	}
	
	public static class BinaryHeapFactory<E> extends HeapFactory<E> {
		@Override
		public Heap<E> makeHeap(Comparator<? super E> comparator) {
			return new BinaryHeap<>(comparator);
		}

		@Override
		public Heap<E> makeHeap(Comparator<? super E> comparator,
				Collection<E> elements) {
			return new BinaryHeap<>(elements);
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static FibonacciHeapFactory fibonacciHeap() {
		return new FibonacciHeapFactory<>();
	}
	
	public static <E> FibonacciHeapFactory<E> fibonacciHeap(Class<E> c) {
		return new FibonacciHeapFactory<>();
	}
	
	
	public static class FibonacciHeapFactory<E> extends HeapFactory<E> {

		@Override
		public Heap<E> makeHeap(Comparator<? super E> comparator) {
			return new FibonacciHeap<>(comparator);
		}

		@Override
		public Heap<E> makeHeap(Comparator<? super E> comparator,
				Collection<E> elements) {
			return new FibonacciHeap<>(elements);
		}
		
	}
	
}
