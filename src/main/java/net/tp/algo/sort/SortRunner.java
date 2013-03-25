package net.tp.algo.sort;

import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

/**
 * User: Trung
 * Date: 3/21/13
 * Time: 11:21 PM
 */
public class SortRunner {



    public final static void write() {
        W++;
    }

    public final static void read() {
        W++;
    }

    public static long W;
    public static long R;

    public static boolean recordRead = true;

    public static boolean recordWrite = true;

    public static void main(String ... args) {

        int N = (1024 << 14) + 100; // adding 100 as noise, because some sorting algorithm has advantage when the size is power of 2

        grandTest("Nearly sorted array", prepareNearSortedArray(N));
        grandTest("Nearly reversely sorted array", prepareNearReverseSortedArray(N));
        grandTest("Random array", prepareArray(N));
        grandTest("Many dup array", prepareManyDupArray(N));


    }

    public static void grandTest(String name, int[] a) {
        System.out.println(name);
        System.out.println(String.format("%20s %20s %20s %20s %20s", "", "N (K)", "D (s)", "Read (K)", "Write (K)"));
        
        test(BucketSort.class, "countingsort", a);
        test(BucketSort.class, "radixsort", a);
        test(QuickSort.class, "quicksort", a);
        test(QuickSort.class, "quicksort3way", a);
        test(QuickSort.class, "quicksort2", a);
        test(IntroSort.class, "introsort", a);
        test(HeapSort.class, "heapsort", a);
        test(SmoothSort.class, "smoothsort", a);
        test(MergeSort.class, "mergesort", a);
        test(MergeSort.class, "bottomUpMergesort", a);

        System.out.println();
    }

    private static void test(Class<?> sorterClass, String methodName, int[] a) {

        int[] testArray = new int[a.length];
        System.arraycopy(a, 0, testArray, 0, a.length);


        Class<?> c = getProxyClass(sorterClass);
        try {
            Method method = c.getMethod(methodName, int[].class);
            W = 0; R = 0;
            long startTime = System.nanoTime();
            method.invoke(null, new Object[]{testArray});
            long duration = (System.nanoTime() - startTime) / 1000000;

            if (!verifySorted(testArray)) {
                System.out.println(String.format("%20s %20s %20.2f %20d %20d", methodName, "FAILED", ((float)(duration))/1000F , R / 1024, W / 1024));
            }
            else {
                System.out.println(String.format("%20s %20d %20.2f %20d %20d", methodName, a.length / 1024, ((float)(duration))/1000F , R / 1024, W / 1024));
            }

        } catch (NoSuchMethodException e) {
            System.out.println(String.format("%20s %20s", methodName, "NOT FOUND"));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException)e.getTargetException();
            }
            else {
                throw new RuntimeException(e.getTargetException());
            }
        }
    }

    private static boolean verifySorted(int[] a) {
        int N = a.length;
        for (int i = 1; i < N; i++) {
            if (a[i-1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static int[] prepareArray(int N) {
        int[] a = new int[N];

        Random random = new Random(2012);
        for (int i = 0; i < N; i++) {
            int randomIndex = random.nextInt(i + 1);
            if (randomIndex != i) {
                a[i] = a[randomIndex];
                a[randomIndex] = i;
            }
            else {
            	a[i] = i;
            }
        }

        return a;
    }

    private static int[] prepareNearSortedArray(int N) {
        int[] a = new int[N];

        Random random = new Random(39821);
        for (int i = 0; i < N; i++) {
            a[i] = i;
        }

        for (int i = (int)Math.log(N); i>= 0; i--) {
            int i1 = random.nextInt(N);
            int i2 = random.nextInt(N);
            int tmp = a[i1];
            a[i1] = a[i2];
            a[i2] = tmp;
        }

        return a;
    }

    private static int[] prepareNearReverseSortedArray(int N) {
        int[] a = new int[N];

        Random random = new Random(39821);
        for (int i = 0; i < N; i++) {
            a[i] = N - i - 1;
        }

        for (int i = (int)Math.log(N); i>= 0; i--) {
            int i1 = random.nextInt(N);
            int i2 = random.nextInt(N);
            int tmp = a[i1];
            a[i1] = a[i2];
            a[i2] = tmp;
        }

        return a;
    }

    private static int[] prepareManyDupArray(int N) {
        int[] a = new int[N];

        int numDup = N / 16;

        int k = 0;
        int j = 0;
        while (j < N) {
            for (int i = 0; i < numDup; i++) {
                if (j >= N) {
                    break;
                }
                a[j++] = k;
            }
            k++;
        }

        Random random = new Random(3123123);
        for (int i = 1; i < N; i++) {
            int randomIndex = random.nextInt(i+1);
            if (randomIndex != i) {
                int tmp = a[i];
                a[i] = a[randomIndex];
                a[randomIndex] = tmp;
            }
        }

        return a;
    }





    private static Class<?> getProxyClass(final Class<?> c) {
        String path = c.getName().replaceAll("\\.", "/") + ".class";
        byte[] classBytes;
        try (InputStream r = SortRunner.class.getClassLoader().getResourceAsStream(path);
             ByteArrayOutputStream bao = new ByteArrayOutputStream()) {

            byte[] buf = new byte[1024];
            int read;
            while ((read = r.read(buf)) > 0) {
                bao.write(buf, 0, read);
            }

            classBytes = bao.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load class " + c.getName());
        }

        ClassReader cr = new ClassReader(classBytes);
        MyClassWriter cw = new MyClassWriter(0);
        cr.accept(cw, 0);

        final byte[] newBytes = cw.toByteArray();

        ClassLoader cl = new MyClassLoader(c.getName(), newBytes);
        try {
            return cl.loadClass(c.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class " + c.getName());
        }
    }


    public static class MyClassWriter extends ClassWriter {

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MyMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
        }

        public MyClassWriter(int i) {
            super(i);
        }

    }

    public static class MyMethodAdapter extends MethodAdapter {

        public MyMethodAdapter(MethodVisitor methodVisitor) {
            super(methodVisitor);
        }

        @Override
        public void visitInsn(int i) {
            super.visitInsn(i);
            if (recordWrite && i == Opcodes.IASTORE) {
                mv.visitFieldInsn(GETSTATIC, "net/tp/algo/sort/SortRunner", "W", "J");
                mv.visitInsn(LCONST_1);
                mv.visitInsn(LADD);
                mv.visitFieldInsn(PUTSTATIC, "net/tp/algo/sort/SortRunner", "W", "J");
//                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/tp/algo/sort/SortRunner", "write", "()V");
            }
            else if (recordRead && i == Opcodes.IALOAD) {
                mv.visitFieldInsn(GETSTATIC, "net/tp/algo/sort/SortRunner", "R", "J");
                mv.visitInsn(LCONST_1);
                mv.visitInsn(LADD);
                mv.visitFieldInsn(PUTSTATIC, "net/tp/algo/sort/SortRunner", "R", "J");
//                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/tp/algo/sort/SortRunner", "read", "()V");
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack+4, maxLocals);
        }
    }


    public static class MyClassLoader extends ClassLoader {

        private String name;
        private byte[] bytes;

        public MyClassLoader(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {

            if (name.equals(this.name)) {
                Class<?> c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }

            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            if (name.equals(this.name)) {
                return defineClass(this.name, this.bytes, 0, this.bytes.length);
            }
            return super.findClass(name);
        }

    }


}
