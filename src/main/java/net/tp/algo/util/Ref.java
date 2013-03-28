package net.tp.algo.util;

import net.tp.algo.tree.BinaryHeap;

/**
 * 
 * Pointer to some internal structure. It's in heap to give client an option to do some operations quickly.
 * 
 * @see BinaryHeap#remove(Ref)
 * @author Trung Phan
 *
 */
public interface Ref {

	public boolean valid();
	
}
