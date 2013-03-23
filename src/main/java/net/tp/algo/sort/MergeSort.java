package net.tp.algo.sort;

/**
 * User: Trung Phan
 * Date: 3/22/13
 * Time: 6:02 PM
 */
public class MergeSort {

    public static void mergesort(int[] a) {
        mergesort(a, 0, a.length - 1);
    }

    private static void mergesort(int[] a, int lo, int hi) {
        if (hi <= lo) {
            return;
        }

        int mid = ((lo + hi) >>> 1) + 1;
        mergesort(a, lo, mid-1);
        mergesort(a, mid, hi);
        
        merge(a, lo, mid, hi);
    }
    
    public static void bottomUpMergesort(int[] a) {
        int N = a.length;

        for (int sz = 1; sz < N ; sz <<= 1) {

            for (int hi = N - 1; hi >= sz ; hi -= 2*sz) {
                int mid = hi + 1 - sz;
                int lo = Math.max(0, hi + 1 - 2*sz);

                merge(a, lo, mid, hi);
            }

        }
    }

    /**
     * Merge 2 sub-array [lo,mid-1] and [mid,hi]
     * 
     * @param a
     * @param lo
     * @param mid lo < mid <= hi
     * @param hi
     */
    private static void merge(int[] a, int lo, int mid, int hi) {
        if (a[mid-1] <= a[mid]) {
            return;
        }

        int bufLen = mid - lo;
        int[] buf = new int[bufLen];
        System.arraycopy(a, lo, buf, 0, bufLen);

        int left = lo, right = mid;
        for (int i = lo; i <= hi; i++) {
            if (left == mid) {
                break;
            }
            else if (right > hi) {
                System.arraycopy(buf, left - lo, a, i, hi - i + 1);
                break;
            }
            else if (buf[left-lo] <= a[right]) {
                a[i] = buf[left++ - lo];
            }
            else {
                a[i] = a[right++];
            }
        }
    }
}
