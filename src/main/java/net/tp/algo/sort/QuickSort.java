package net.tp.algo.sort;

import java.util.Random;

/**
 * User: Trung Phan
 * Date: 3/20/13
 * Time: 7:49 AM
 */
public class QuickSort {

	/**
	 * Shuffle the array before doing quicksort.
	 * 
	 * @param a
	 */
    public static void shufflequicksort(int[] a) {
        shuffle(a);
        quicksort(a, 0, a.length - 1);
    }


	private static void shuffle(int[] a) {
		int N = a.length;
        Random random = new Random();
        for (int i = 1; i < N; i++) {
            int randomIndex = random.nextInt(i+1);
            if (randomIndex != i) {
                int tmp = a[i];
                a[i] = a[randomIndex];
                a[randomIndex] = tmp;
            }
        }
	}


    public static void quicksort(int[] a) {
        quicksort(a, 0, a.length - 1);
    }

    private static void quicksort(int[] a, int lo, int hi) {
        while (hi > lo) {
            int pi = selectPivot(a, lo, hi);
            int pivot = a[pi];
            a[pi] = a[lo]; a[lo] = pivot;

            int lt = lo+1, gt = hi;

            while (true) {
                while (lt < hi && a[lt] < pivot) lt++;
                while (gt >= lt && a[gt] > pivot) gt--;
                if (lt > gt) {
                    a[lo] = a[--lt]; a[lt] = pivot;
                    break;
                }
                swap(a, lt++, gt--);
            }

            if (lt-lo < hi - gt) { // put the smaller sub problem to the stack
                quicksort(a, lo, lt-1);
                lo = gt+1;
            }
            else {
                quicksort(a, gt+1, hi);
                hi = lt-1;
            }
        }
    }


    public static void quicksort3way(int[] a) {
        quicksort3way(a, 0, a.length-1);
    }

    /**
     *
     * Implement 3-way partition quicksort. See http://www.sorting-algorithms.com/quick-introsort-3-way.
     * This implementation makes sure that it's fast for array of duplicate data.
     *
     * User: Trung
     * Date: 3/21/13
     * Time: 9:54 PM
     */
    private static void quicksort3way(int[] a, int lo, int hi) {

        while (hi > lo) {
            int pivot = a[selectPivot(a, lo, hi)];
            int lt = lo, gt = hi, i = lt;
            while (i <= gt) {
                int v = a[i];
                if (v < pivot) {
                    a[i++] = a[lt]; a[lt++] = v;
                }
                else if (v > pivot) {
                    a[i] = a[gt]; a[gt--] = v;
                }
                else {
                    i++;
                }
            }

            if (lt-lo < hi - gt) { // put the smaller sub problem to the stack
                quicksort3way(a, lo, lt-1);
                lo = gt+1;
            }
            else {
                quicksort3way(a, gt+1, hi);
                hi = lt-1;
            }
        }

    }

    /**
    *
    * Improve on quicksort, but also handle duplicate nicely.
    * 
    * see http://golang.org/src/pkg/sort/sort.go?s=4375:4400#L179, which is based on "Engineering a Sort Function"
    * @param a
    * @param lo
    * @param hi
    */
    public static void quicksort2(int[] a) {
        quicksort2(a, 0, a.length-1);
    }

    private static void quicksort2(int[] a, int lo, int hi) {
    	
    	while (hi > lo) {
    		int pi = selectPivot(a, lo, hi);
    		swap(a, lo, pi);
    		int pivot = a[lo];
    		
    		int lt = lo+1, b=lt, c = hi, gt = hi;
    		while (true) {
    			while (b <= c && a[b] < pivot) b++;
    			while (c >= b && a[c] > pivot) c--;
    			if (b > c) {
    				break;
    			}
    			
    			if (a[b] == pivot) {
    				swap(a, lt++, b++);
    				continue;
    			}
    			if (a[c] == pivot) {
    				swap(a, gt--, c--);
    				continue;
    			}
    			swap(a, b++, c--);
    		}
    		int n = Math.min(lt-lo, b-lt);
    		swapRange(a, lo, b-n, n);
    		n = Math.min(hi-gt, gt-c);
    		swapRange(a, c+1, hi+1-n, n);
    		
    		if (b - lt < gt - c) {
    			quicksort2(a, lo, lo + b - lt - 1);
    			lo = hi + c - gt + 1;
    		} else {
    			quicksort2(a, hi + c - gt + 1, hi);
    			hi = lo + b - lt - 1;
    		}
    	}
    }

    private static int selectPivot(int[] a, int lo, int hi) {

        int mid = (lo+hi) >>> 1;
        int l = hi - lo;
        if (l < 40) {
            return medianOf3(a, lo, mid, hi);
        }
        else {
            // using Turkey's Ninther
            int s = l / 8;
            return
                    medianOf3(a,
                            medianOf3(a, lo, lo + s, lo + 2*s),
                            medianOf3(a, mid, mid-s, mid+s),
                            medianOf3(a, hi-2*s, hi-s, hi)
                    );

        }
    }

    private static int medianOf3(int[] a, int x, int y, int z) {
        if (a[x] <= a[y]) {
            if (a[z] <= a[x]) {
                return x;
            }
            return a[y] <= a[z] ? y : z;
        } else if (a[z] <= a[y]) {
            return y;
        }
        return a[x] <= a[z] ? x : z;
    }
    
    private static void swap(int [] a, int i, int j) {
    	if (i!=j) {
    		int tmp = a[i];
    		a[i] = a[j];
    		a[j] = tmp;
    	}
    }
    
    private static void swapRange(int[] a, int x, int y, int n) {
    	for (; n > 0; n--) {
    		int tmp = a[x];
    		a[x++] = a[y];
    		a[y++] = tmp;
    	}
    }
}
