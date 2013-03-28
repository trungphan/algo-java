package net.tp.algo.tree;

import net.tp.algo.util.Ref;

public interface Heap<E> {
	
	public E head();
	
	/**
	 * This operation takes O(log(n)) times.
	 * @return
	 */
	public E removeHead();
	
	public Ref add(E element);

	public E remove(Ref ref);

	public void clear();
	
	public boolean empty();
	
	public int size();
	
	public Ref find(E element);
	
	public void update(Ref ref);
}
