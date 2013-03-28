package net.tp.algo.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import junit.framework.Assert;

import net.tp.algo.util.NaturalComparator;
import net.tp.algo.util.Ref;

import org.junit.Test;

/**
 * 
 * This is min-heap. To make a max-heap, pass a reverse natural order comparator.
 * 
 * @author Trung Phan
 *
 */
public class BinaryHeap<E> implements Heap<E> {

	private final Comparator<? super E> comparator;
	private Entry<E>[] elements;
	private int n;

	public BinaryHeap() {
		this(null, null);
	}
	
	public BinaryHeap(Collection<E> elements) {
		this(null, elements);
	}
	
	public BinaryHeap(Comparator<? super E> comparator) {
		this(comparator, null);
	}
	
	/**
	 * 
	 * It takes O(n) time to build the heap from existing collection
	 * 
	 * @param comparator
	 * @param elements
	 */
	@SuppressWarnings("unchecked")
	public BinaryHeap(Comparator<? super E> comparator, Collection<E> elements) {
		this.comparator = comparator;
		if (elements != null) {
			this.elements = new Entry[elements.size()];
			for (E element : elements) {
				this.elements[n] = new Entry<E>(this, element, n);
				n++;
			}
			heapify();
		}
		else {
			this.elements = new Entry[16];
		}
	}
	
	/**
	 * 
	 * This operation takes O(n) times.
	 * 
	 */
	private void heapify() {
		for (int i = n / 2 - 1; i >= 0; i--) {
			siftDown(i);
		}
	}
	
	/**
	 * This operation takes O(log(n)) times. The sister function siftDown also takes O(log(n)) times, but this function
	 * makes less comparison, hence is faster. This difference is more prominent if using a d-ary heap instead of
	 * binary heap. Therefore, if the main operations is decreasing priority, d-ary heap has an advantage over this
	 * binary heap.
	 * 
	 * @param i
	 */
	private void siftUp(int i) {
		Entry<E> v = elements[i];
		while (true) {
			int pi = i > 0 ? (i - 1) / 2 : -1;
			
			if (pi < 0) {
				break;
			}
			
			if (compare(v.element, elements[pi].element) >= 0) {
				break;
			}
			
			elements[i] = elements[pi];
			elements[i].pos = i;
			i = pi;
		}

		elements[i] = v;
		elements[i].pos = i;		
	}
	
	/**
	 * This operation takes O(log(n)) times.
	 * 
	 * @see Heap#siftUp(int)
	 * @param i
	 */
	private void siftDown(int i) {

		Entry<E> v = elements[i];
		
		while (true) {
			int ci = i * 2 + 1;
			if (ci >= n) {
				break;
			}
			if (ci < n - 1 && compare(elements[ci].element, elements[ci+1].element) > 0) {
				ci++;
			}
			
			if (compare(v.element, elements[ci].element) <= 0) {
				break;
			}
			
			this.elements[i] = this.elements[ci];
			this.elements[i].pos = i;
			i = ci;
		}
		
		this.elements[i] = v;
		this.elements[i].pos = i;
	}
	
	public boolean empty() {
		return n == 0;
	}
	
	@SuppressWarnings("unchecked")
	public void clear() {
		for (int i = 0; i < n; i++) {
			elements[i].heap = null;
			elements[i].element = null;
			elements[i] = null;
		}
		elements = new Entry[16];
		n = 0;
	}
	
	/**
	 * This operation takes O(1) times.
	 * @return
	 */
	public E head() {
		if (n == 0) {
			throw new NoSuchElementException();
		}
		
		return elements[0].element;
	}
	
	/**
	 * This operation takes O(log(n)) times.
	 * @return
	 */
	public E removeHead() {
		if (n == 0) {
			throw new NoSuchElementException();
		}
		
		E result = elements[0].element;
		
		swap(0, n-1);
		elements[n-1].heap = null;
		elements[n-1] = null;
		n--;
		
		if (n > 0) {
			siftDown(0);
		}
		
		return result;
	}
	
	/**
	 * 
	 * This operation takes O(log(n)) times. The client can keep the ref to remove the element later.
	 * This can heap algorithm like Dijkstra's shortest path to quickly remove edges and update weight.
	 * 
	 * @param element
	 * @return
	 */
	public Ref add(E element) {
		
		if (element == null) {
			throw new NullPointerException();
		}
		
		if (elements.length == n) {
			elements = Arrays.copyOf(elements, elements.length * 2);
		}
		
		n++;
		Entry<E> entry = new Entry<E>(this, element, n-1);
		elements[n-1] = entry;
		siftUp(n-1);

		return entry;
	}

	/**
	 * 
	 * This operation takes O(log(n)) times.
	 * 
	 * @param ref
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E remove(Ref ref) {
		if (ref instanceof Entry) {
			Entry<E> entry = (Entry<E>)ref;
			if (entry.heap == this) {
				int i = entry.pos;
				E result = entry.element;

				swap(i, n - 1);
				elements[n-1].heap = null;
				elements[n-1] = null;
				n--;
				if (n > 0) {
					siftDown(i);
				}
				
				return result;
			}
		}
		
		return null;
	}
	
	/**
	 * Return the number of elements.
	 * @return
	 */
	public int size() {
		return n;
	}
	
	
	/**
	 * 
	 * This operation takes O(n) times. The main purpose of using this ref is to delete the element in the heap.
	 * The client is encouraged to keep the ref when adding an element into this heap instead of finding the ref
	 * using this method because this method takes O(n) times while the delete(ref) takes only O(log(n)) times.
	 * 
	 * @param element
	 * @return
	 */
	public Ref find(E element) {
		for (int i = 0; i < n; i++) {
			if (compare(elements[i].element, element) == 0) {
				return elements[i];
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private int compare(E element1, E element2) {
		return comparator != null ? comparator.compare(element1, element2) : ((Comparable<E>)element1).compareTo(element2);
	}
	
	private void swap(int i, int j) {
		if (i != j) {
			Entry<E> tmp = elements[i];
			elements[i] = elements[j];
			elements[j] = tmp;
			
			elements[i].pos = i;
			elements[j].pos = j;
		}
	}
	
	private static class Entry<K> implements Ref {
		private BinaryHeap<K> heap;
		private K element;
		private int pos;
		
		private Entry(BinaryHeap<K> heap, K element, int pos) {
			if (element == null) {
				throw new NullPointerException();
			}
			this.heap = heap;
			this.element = element;
		}
		
		public boolean valid() {
			return this.heap != null;
		}
		
		@Override
		public String toString() {
			return element.toString();
		}
		
	}
	
	
	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOf(elements, n));
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * 
	 * Test Cases
	 * 
	 * @author Trung Phan
	 *
	 */
	public static class TestCase {
		
		@Test
		public void heapify_one_element() {
			BinaryHeap<Integer> heap = new BinaryHeap<>(new NaturalComparator<Integer>(), Arrays.asList(5));
			Assert.assertFalse(heap.empty());
			Assert.assertEquals(Integer.valueOf(5), heap.head());
			Assert.assertEquals(Integer.valueOf(5), heap.removeHead());
			Assert.assertTrue(heap.empty());
		}
		
		@SuppressWarnings("unchecked")
		@Test
		public void remove_ref_of_one_element_heap() {
			BinaryHeap<Integer> heap = new BinaryHeap<Integer>(new NaturalComparator<Integer>());
			Ref ref = heap.add(1);
			
			Assert.assertEquals(Integer.valueOf(1), heap.head());
			Assert.assertEquals(1, heap.size());
			
			Integer element = heap.remove(ref);
			Assert.assertNotNull(element);
			Assert.assertNull(((Entry<Integer>)ref).heap);
			Assert.assertTrue(heap.empty());
		}

		@Test
		public void remove_invalid_ref_should_return_false() {
			BinaryHeap<Integer> heap = new BinaryHeap<Integer>(new NaturalComparator<Integer>());
			Ref ref = heap.add(1);
			heap.removeHead();
			
			Integer element = heap.remove(ref);
			Assert.assertNull(element);
		}
		
		@Test
		public void random_test() {
			BinaryHeap<Integer> heap = new BinaryHeap<>();
			
			List<Integer> list = new ArrayList<>();
			Random random = new Random(100);
			int size = random.nextInt(100);
			for (int i = 0; i < size; i++) {
				list.add(0);
				int rand = random.nextInt(i + 1);
				if (i != rand) {
					list.set(i, list.get(rand));
					list.set(rand, i);
				}
				else {
					list.set(i, i);
				}
			}
			
			for (int i = 0; i < size; i++) {
				heap.add(list.get(i));
			}
			
			Collections.sort(list);
			
			for (int i = 0; i < size; i++) {
				Assert.assertEquals(list.get(i), heap.removeHead());
			}
		}
		
		
	}


	@Override
	public void update(Ref ref) {
		if (ref instanceof Entry) {
			Entry<E> entry = (Entry<E>)ref;
			if (entry.heap == this) {
				int i = entry.pos;
				update(i);
			}
		}
	}

	private void update(int i) {
		int pi = i > 0 ? (i-1) / 2 : -1;
		
		if (i > 0 && compare(elements[i].element, elements[pi].element) < 0) {
			siftUp(i);
		}
		else {
			siftDown(i);
		}
	}
	
	
	
}
