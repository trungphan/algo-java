package net.tp.algo.sort;

import java.util.Arrays;

public class BucketSort {

	public static void countingsort(int[] a) {
		int N = a.length;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i = 0; i < N; i++) {
			int v = a[i];
			if (min > v) min = v;
			if (max < v) max = v;
		}
		
		int[] buckets = new int[max - min + 1]; // counting bucket
		
		for (int i = 0; i < N; i++) {
			int bi = a[i] - min;
			buckets[bi]++;
		}
		
		for (int i = 1; i < buckets.length; i++) {
			buckets[i] += buckets[i-1];
		}
		
		int[] b = new int[N];
		for (int i = N - 1; i >= 0; i--) {
			int bi = a[i] - min;
			buckets[bi]--;
			b[buckets[bi]] = a[i];
		}
		
		System.arraycopy(b, 0, a, 0, N);
	}
	
	public static void radixsort(int[] a) {
		int N = a.length;
		int[] b = new int[N];
		int[] buckets = new int[16]; // 4 bit
		boolean flag = true; // flag to indicate if a or b should be sorted.
		
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i = 0; i < N; i++) {
			int v = a[i];
			if (min > v) min = v;
			if (max < v) max = v;
		}
		int bits = 32 - Integer.numberOfLeadingZeros(max - min);
		
		for (int rshift = 0; rshift < bits; rshift+= 4, flag = !flag) {
			int[] src = flag ? a : b;
			int[] dest = flag ? b : a;
			
			Arrays.fill(buckets, 0);
			
			for (int i = 0; i < N; i++) {
				int bi = ((src[i] - min) >>> rshift) & 0xF;
				buckets[bi]++;
			}
			
			for (int i = 1; i < buckets.length; i++) {
				buckets[i] += buckets[i-1];
			}
			for (int i = N-1; i >= 0; i--) {
				int bi = ((src[i] - min) >>> rshift) & 0xF;
				buckets[bi]--;
				dest[buckets[bi]] = src[i];
			}
		}
		
		if (!flag) {
			System.arraycopy(b, 0, a, 0, N);
		}
		
	}
	
	
	
	public static void main(String ... args) {
		
		int[] a = {Integer.MIN_VALUE, 103, 107, 104, 105, 101, -103, -107, -104, -105, -101, Integer.MAX_VALUE};
		
		radixsort(a);
		
		System.out.println(Arrays.toString(a));
		
		a = new int[]{-103, -107, -104, -105, -101, 104, 108};
		radixsort(a);
		
		System.out.println(Arrays.toString(a));
		
	}
	
}
