package net.tp.algo.sort;

/**
 * User: Trung Phan
 * Date: 3/20/13
 * Time: 7:47 AM
 */
public class SmoothSort {


    /**
     * L[0] = L[1] = 1.
     * L[n] = L[n-1] + L[n-2] + 1
     */
    static final int L[] = { 1, 1, 3, 5, 9, 15, 25, 41, 67, 109, 177, 287, 465, 753,
            1219, 1973, 3193, 5167, 8361, 13529, 21891, 35421, 57313, 92735, 150049,
            242785, 392835, 635621, 1028457, 1664079, 2692537, 4356617, 7049155,
            11405773, 18454929, 29860703, 48315633, 78176337, 126491971, 204668309,
            331160281, 535828591, 866988873
    };

    public static void smoothsort(int[] a) {

        int N = a.length;
        int orders[] = new int[(int)(Math.log(N) / Math.log(2)) * 2];
        int trees = 0;

        for (int i = 0; i < N; i++) {
            if (trees > 1 && orders[trees-2] == orders[trees-1] + 1) {
                trees--;
                orders[trees-1]++;
            }
            else if (trees > 0 && orders[trees-1] == 1) {
                orders[trees++] = 0;
            }
            else {
                orders[trees++] = 1;
            }
            findAndSift(a, i, trees-1, orders);
        }

        for (int i = N-1; i > 0; i--) {
            if (orders[trees-1] <= 1) {
                trees--;
            }
            else {
                int ri = i-1;
                int li = ri - L[orders[trees-1] - 2];

                trees++;
                orders[trees-2]--;
                orders[trees-1] = orders[trees-2]-1;

                findAndSift(a, li, trees-2, orders);
                findAndSift(a, ri, trees-1, orders);
            }
        }

    }

    private static void findAndSift(int[] a, int i, int tree, int[] orders) {
        int v = a[i];
        while (tree > 0) {
            int pi = i - L[orders[tree]];
            if (a[pi] <= v) {
                break;
            }
            else if (orders[tree] > 1) {
                int ri = i-1;
                int li = ri - L[orders[tree]-2];
                if (a[pi] <= a[li] || a[pi] <= a[ri]) {
                    break;
                }
            }

            a[i] = a[pi];
            i = pi;
            tree--;
        }
        a[i] = v;
        siftDown(a, i, orders[tree]);
    }

    private static void siftDown(int[] a, int i, int order) {
        int v = a[i];
        while (order > 1) {
            int ri = i-1;
            int li = ri - L[order-2];
            if (v >= a[li] && v >= a[ri]) {
                break;
            }
            else if (a[li] <= a[ri]) {
                a[i] = a[ri];
                i = ri;
                order -= 2;
            }
            else {
                a[i] = a[li];
                i = li;
                order -= 1;
            }
        }
        a[i] = v;
    }


    public static void main(String... args) {

        int[] a = {2,4,5,3,1};

        smoothsort(a);
        for (int i = 0; i < a.length; i++) {
            System.out.print(String.format("%3d", a[i]));
        }
    }

}
