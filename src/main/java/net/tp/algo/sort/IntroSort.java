package net.tp.algo.sort;

import java.util.Random;

/**
 * User: Trung Phan
 * Date: 3/20/13
 * Time: 7:25 AM
 */
public class IntroSort
{


    public static void introsort(int[] a) {
        introsort(a, 0, a.length-1, (int)(Math.log(a.length)/Math.log(2)) * 2);
    }

    /**
     *
     *
     *
     * @param a
     * @param lo
     * @param hi
     * @param level
     */
    public static void introsort(int[] a, int lo, int hi, int level) {

        while (hi - lo > 47) {
            if (level == 0) {
                heapsort(a, lo, hi);
                return;
            }
            level--;

            long l = partition(a, lo, hi);
            int le = (int)(l >>> 32);
            int ge = (int)l;
            if (le - lo < hi - ge) {
            	introsort(a, lo, le, level);
            	lo = ge;
            }
            else {
            	introsort(a, ge, hi, level);
            	hi = le;
            }
        }

        insertsort(a, lo, hi);

    }
    
    private static long partition(int[] a, int lo, int hi) {
        int pi = selectPivot(a, lo, hi);
        swap(a, lo, pi);
        int pivot = a[lo];

		int lt = lo+1, i=lt, j = hi, gt = hi;
		while (true) {
			while (i <= j && a[i] < pivot) i++;
			while (j >= i && a[j] > pivot) j--;
			if (i > j) {
				break;
			}
			
			if (a[i] == pivot) {
				swap(a, lt++, i++);
				continue;
			}
			if (a[j] == pivot) {
				swap(a, gt--, j--);
				continue;
			}
			swap(a, i++, j--);
		}
		int n = Math.min(lt-lo, i-lt);
		swapRange(a, lo, i-n, n);
		n = Math.min(hi-gt, gt-j);
		swapRange(a, j+1, hi+1-n, n);
		
		return ((long)(lo + i - lt - 1)) << 32 | (hi + j - gt + 1);
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


    private static void heapsort(int[] a, int lo, int hi) {
        for (int i = (lo+hi) >>> 1; i >= lo; i--) {
            siftDown(a, i, lo, hi);
        }

        for (int i = hi; i > lo; i--) {
            int tmp = a[i]; a[i] = a[lo]; a[lo] = tmp;
            siftDown(a, lo, lo, i-1);
        }
    }


    private static void siftDown(int[] a, int i, int lo, int hi) {

        int v = a[i];
        while (true) {
            int ci = (i - lo) * 2 + 1 + lo;
            if (ci > hi) {
                break;
            }
            if (ci < hi && a[ci] <= a[ci+1]) {
                ci++;
            }
            if (v >= a[ci]) {
                break;
            }
            a[i] = a[ci];
            i = ci;
        }
        a[i] = v;

    }



    public static void insertsort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int v = a[i];
            int j;
            for (j = i; j > lo && v < a[j-1]; j--) {
                a[j] = a[j-1];
            }
            if (i != j) {
                a[j] = v;
            }
        }
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

    
    public static void main(String ... args) {

        int N = 1024 << 4;
        int[] a = new int[N];
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            int ri = random.nextInt(i + 1);
            if (ri != i) {
                a[i] = a[ri]; a[ri] = i;
            }
            else {
                a[i] = i;
            }
        }

        introsort(a, 0, a.length-1, 10);

        for (int i = 0; i < N-1; i++) {
            if (a[i] > a[i+1]) {
                throw new IllegalStateException();
            }
        }
    }


}