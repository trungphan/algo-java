package net.tp.algo.sort;

/**
 * 
 * @author Trung Phan
 *
 */
public class HeapSort {

	public static void heapsort(int[] a) {
		int N = a.length;
		heapify(a, N);
		
		for (int i = N-1; i > 0; i--) {
			swap(a, 0, i);
			siftDown(a, 0, i);
		}
		
	}

	/**
	 * 
	 * O(n) time.
	 * 
	 * @param a
	 * @param N
	 */
	private static void heapify(int[] a, int N) {
		for (int i = N/2 - 1; i >= 0; i--) {
			siftDown(a, i, N);
		}
	}
	
	private static void siftDown(int[] a, int i, int end) {
		int v = a[i];
		while (true) {
			int c = 2*i + 1;
			if (c >= end) {
				break;
			}
			if (c+1 < end && a[c] < a[c+1]) {
				c++;
			}
			if (v >= a[c]) {
				break;
			}
			a[i] = a[c];
			i = c;
		}
		a[i] = v;
	}
	
	private static void swap(int[] a, int i, int j) {
		int tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
	
}
