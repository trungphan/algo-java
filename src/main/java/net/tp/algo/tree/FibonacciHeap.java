package net.tp.algo.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import net.tp.algo.util.NaturalComparator;
import net.tp.algo.util.Ref;

public class FibonacciHeap<E> implements Heap<E> {

	private final Comparator<? super E> comparator;

	private Node<E> min;
	
	private int n;
	
	public FibonacciHeap() {
		this(null, null);
	}
	
	public FibonacciHeap(Collection<E> elements) {
		this(null, elements);
	}

	public FibonacciHeap(Comparator<? super E> comparator) {
		this(comparator, null);
	}
	
	public FibonacciHeap(Comparator<? super E> comparator, Collection<E> elements) {
		this.comparator = comparator;
		
		if (elements != null) {
			for (E element : elements) {
				add(element);
			}
		}
	}	
	
	private static class Node<E> implements Ref {
		private FibonacciHeap<E> heap;
		private Node<E> parent;
		private Node<E> left;
		private Node<E> right;
		private Node<E> child;
		private int rank; // number of children (not counting grand children)
		private boolean marked;
		
		private Node(FibonacciHeap<E> heap, E element) {
			this.heap = heap;
			this.element = element;
			this.left = this;
			this.right = this;
		}
		
		E element;

		private void addChild(Node<E> c) {
			this.rank++;
			if (this.child == null) {
				this.child = c;
				c.left = c; c.right = c; c.parent = this;
			}
			else {
				addSibling(c);
			}
		}
		
		private void addSibling(Node<E> c) {
			
			Node<E> rightSib = this.right;
			Node<E> cRightSib = c.right;
			this.right = cRightSib;
			c.right = rightSib;
			rightSib.left = c;
			cRightSib.left = this;
		}
		
		private void detach() {
			Node<E> p = parent;
			if (p != null) {
				p.rank --;
			}
			parent = null;
			
			if (right == this) {
				if (p != null) {
					p.child = null;
				}
			}
			else {
				if (p != null && p.child == this) {
					p.child = right;
				}
				left.right = right;
				right.left = left;
				left = this;
				right = this;
			}
		}
		
		private Node<E> merge(Node<E> n) {
			if (this == n) {
				return this;
			}
			if (this == n || this.parent != n.parent || n.heap != this.heap) {
				throw new IllegalStateException();
			}
			
			if (heap.compare(element, n.element) <= 0) {
				n.detach();
				addChild(n);
				return this;
			}
			else {
				n.detach();
				addSibling(n);
				detach();
				n.addChild(this);
				return n;
			}
		}
		
		@Override
		public boolean valid() {
			return this.heap != null;
		}
		
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append(element).append(" <").append(left.element).append(" >").append(right.element);
			if (parent != null) {
				result.append(" ^").append(parent.element);
			}
			if (child != null) {
				result.append(" v").append(child.element).append("{").append(rank).append("}");
			}
			
			return result.toString();
		}
		
	}
	
	@Override
	public E head() {
		if (min == null) {
			throw new NoSuchElementException();
		}
		return min.element;
	}

	@Override
	public E removeHead() {
		
		
		if (min == null) {
			throw new NoSuchElementException();
		}
		
		return removeRoot(min);
	}
	
	private E removeRoot(Node<E> root) {
		if (root == null) {
			throw new NullPointerException();
		}
		if (root.parent != null) {
			throw new IllegalArgumentException();
		}
		
		E result = root.element;
		n--;

		// melt children into root list
		Node<E> child = root.child;
		root.child = null;
		root.heap = null;
		root.element = null;
		
		while (child != null && child.parent != null) {
			child.parent = null;
			child = child.right;
		}
		if (child != null) {
			root.addSibling(child);
		}

		if (root.right == root) {
			min = null;
			return result;
		}
		else {
			min = root.right;
			root.left.right = root.right;
			root.right.left = root.left;
			root.left = null;
			root.right = null;
			root = min;
		}
		
		// update new min
		Node<E> current = root.right;
		while (current != root) {
			if (compare(min.element, current.element) > 0) {
				min = current;
			}
			current = current.right;
		}
		
		consolidateRoots();

		return result;
	}

	// consolidate so that no 2 root nodes have the same rank.
	private void consolidateRoots() {
		Node<E> current = min;
		Map<Integer, Node<E>> ranks = new HashMap<>();
		do {
			Node<E> n;
			while ((n = ranks.get(current.rank)) != null) {
				ranks.remove(current.rank);
				current = current.merge(n);
			}
			ranks.put(current.rank, current);
			
			current = current.right;
		} while (current != min);
	}

	@Override
	public Ref add(E element) {
		Node<E> node = new Node<E>(this, element);
		if (min == null) {
			min = node;
		}
		else {
			min.addSibling(node);
			if (compare(min.element, node.element) > 0) {
				min = node;
			}
		}
		n++;
		return node;
	}
	
	@SuppressWarnings("unchecked")
	private int compare(E e1, E e2) {
		return comparator != null ? comparator.compare(e1, e2) : ((Comparable<E>)e1).compareTo(e2);
	}

	@SuppressWarnings("unchecked")
	@Override
	public E remove(Ref ref) {
		
		if (ref instanceof Node) {
			Node<E> node = (Node<E>)ref;
			if (node.heap == this) {
				// pretend that this key is decreased to Integer.MIN_VALUE
				decreaseKey(node);
				return removeRoot(node);
			}
		}
		
		return null;
	}

	@Override
	public void clear() {
		dispose(min);
	}
	
	private void dispose(Node<E> node) {
		if (node == null) {
			return;
		}
		Node<E> n = node;
		do {
			dispose(n.child);
			n.child = null;
			n.left = null;
			n.parent = null;
			n.element = null;
			n.heap = null;
			Node<E> prev = n;
			prev.right = null;
			n = n.right;
		} while (n != node);
	}

	@Override
	public boolean empty() {
		return n == 0;
	}

	@Override
	public int size() {
		return n;
	}

	@Override
	public Ref find(E element) {
		return find(min, element);
	}
	
	private Node<E> find(Node<E> parent, E searchElement) {
		if (parent == null) {
			return null;
		}
		
		Node<E> n = parent;
		do {
			if (compare(n.element, searchElement) == 0) {
				return n;
			}
			Node<E> result = find(n.child, searchElement);
			if (result != null) {
				return result;
			}
			n = n.right;
		} while (n != parent);
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(Ref ref) {
		
		if (ref instanceof Node) {
			Node<E> node = (Node<E>)ref;
			if (node.heap == this) {
				
				if (node.parent != null && compare(node.parent.element, node.element) > 0) {
					decreaseKey(node);
				}
				else if (node.child != null) {
					Node<E> n = node.child;
					do {
						if (compare(node.element, n.element) > 0) {
							throw new UnsupportedOperationException("Not support increase key yet");
						}
						n = n.right;
					} while (n != node.child);
				}
			}
		}
		

	}

	private void decreaseKey(Node<E> node) {
		Node<E> current = node;
		while (current != null && current.parent != null) {
			Node<E> parent = current.parent;
			current.detach();
			current.marked = false;
			min.addSibling(current);
			if (compare(min.element, current.element) > 0) {
				min = current;
			}
			if (parent.marked) {
				current = parent;
			}
			else {
				parent.marked = true;
				break;
			}
		}
	}
	

	public static class TestCase {
		
		
		@Test
		public void test() {
			
			FibonacciHeap<Integer> heap = new FibonacciHeap<>(new NaturalComparator<Integer>());
			
			heap.add(5);
			heap.add(3);
			heap.add(9);
			
			Assert.assertEquals(3, heap.removeHead().intValue());
			Assert.assertEquals(5, heap.removeHead().intValue());
			Assert.assertEquals(9, heap.removeHead().intValue());
			
			
			
		}
		
		@Test
		public void random_test() {
			FibonacciHeap<Integer> heap = new FibonacciHeap<>(new NaturalComparator<Integer>());
			
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
				Assert.assertEquals(i + 1, heap.size());
			}
			
			Collections.sort(list);
			
			for (int i = 0; i < size; i++) {
				Assert.assertEquals(list.get(i), heap.removeHead());
				Assert.assertEquals(size - i - 1, heap.size());
			}
		}		
		
		
	}
	
}
