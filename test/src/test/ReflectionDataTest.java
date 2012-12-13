package test;

import sun.reflect.annotation.AnnotationType;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ReflectionDataTest {
    public static class Class0 {}

    public static class ClassA extends Class0 {
        public String fa;

        public ClassA() {
        }

        public void m1() {
        }

        public void m2() {
        }

        public void m3() {
        }
    }

    public static class ClassB extends ClassA {
        public String fb;

        public ClassB() {
        }

        public void m1() {
        }

        public void m2() {
        }
    }

    public static class ClassC extends ClassB {
        public String fc;

        public ClassC() {
        }

        public void m1() {
        }
    }

    static abstract class Test extends Thread {
        final int loops;

        protected Test(int loops) {
            this.loops = loops;
        }

        protected abstract void runTest();

        long t0, t;
        Throwable throwable;

        @Override
        public synchronized void start() {
            t0 = System.nanoTime();
            super.start();
        }

        @Override
        public void run() {
            try {
                runTest();
            }
            catch (Throwable thr) {
                throwable = thr;
            }
            finally {
                t = System.nanoTime() - t0;
            }
        }
    }

    static class TestBulkDeclared extends Test {
        public TestBulkDeclared(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            for (int i = 0; i < loops; i++) {
                Objects.requireNonNull(ClassA.class.getDeclaredConstructors());
                Objects.requireNonNull(ClassA.class.getDeclaredMethods());
                Objects.requireNonNull(ClassA.class.getDeclaredFields());
                Objects.requireNonNull(ClassB.class.getDeclaredConstructors());
                Objects.requireNonNull(ClassB.class.getDeclaredMethods());
                Objects.requireNonNull(ClassB.class.getDeclaredFields());
                Objects.requireNonNull(ClassC.class.getDeclaredConstructors());
                Objects.requireNonNull(ClassC.class.getDeclaredMethods());
                Objects.requireNonNull(ClassC.class.getDeclaredFields());
            }
        }
    }

    static class TestBulkPublic extends Test {
        public TestBulkPublic(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            for (int i = 0; i < loops; i++) {
                Objects.requireNonNull(ClassA.class.getConstructors());
                Objects.requireNonNull(ClassA.class.getMethods());
                Objects.requireNonNull(ClassA.class.getFields());
                Objects.requireNonNull(ClassB.class.getConstructors());
                Objects.requireNonNull(ClassB.class.getMethods());
                Objects.requireNonNull(ClassB.class.getFields());
                Objects.requireNonNull(ClassC.class.getConstructors());
                Objects.requireNonNull(ClassC.class.getMethods());
                Objects.requireNonNull(ClassC.class.getFields());
            }
        }
    }

    static class TestByNameDeclared extends Test {
        public TestByNameDeclared(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            try {
                Class<?>[] emptyParams = new Class[0];
                for (int i = 0; i < loops; i++) {
                    ClassA.class.getDeclaredConstructor(emptyParams);
                    ClassA.class.getDeclaredMethod("m1", emptyParams);
                    ClassA.class.getDeclaredMethod("m2", emptyParams);
                    ClassA.class.getDeclaredMethod("m3", emptyParams);
                    ClassA.class.getDeclaredField("fa");

                    ClassB.class.getDeclaredConstructor(emptyParams);
                    ClassB.class.getDeclaredMethod("m1", emptyParams);
                    ClassB.class.getDeclaredMethod("m2", emptyParams);
                    ClassB.class.getDeclaredField("fb");

                    ClassC.class.getDeclaredConstructor(emptyParams);
                    ClassC.class.getDeclaredMethod("m1", emptyParams);
                    ClassC.class.getDeclaredField("fc");
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestByNamePublic extends Test {
        public TestByNamePublic(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            try {
                Class<?>[] emptyParams = new Class[0];
                for (int i = 0; i < loops; i++) {
                    ClassA.class.getConstructor(emptyParams);
                    ClassA.class.getMethod("m1", emptyParams);
                    ClassA.class.getMethod("m2", emptyParams);
                    ClassA.class.getMethod("m3", emptyParams);
                    ClassA.class.getField("fa");

                    ClassB.class.getConstructor(emptyParams);
                    ClassB.class.getMethod("m1", emptyParams);
                    ClassB.class.getMethod("m2", emptyParams);
                    ClassB.class.getMethod("m3", emptyParams);
                    ClassB.class.getField("fa");
                    ClassB.class.getField("fb");

                    ClassC.class.getConstructor(emptyParams);
                    ClassC.class.getMethod("m1", emptyParams);
                    ClassC.class.getMethod("m2", emptyParams);
                    ClassC.class.getMethod("m3", emptyParams);
                    ClassC.class.getField("fa");
                    ClassC.class.getField("fb");
                    ClassC.class.getField("fc");
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static double runTest(Class<? extends Test> testClass, int threads, int loops, double prevTp, boolean reference) {

        try {
            Constructor<? extends Test> constructor = testClass.getConstructor(int.class);

            Test[] tests = new Test[threads];
            for (int i = 0; i < tests.length; i++) {
                tests[i] = constructor.newInstance(loops);
            }

            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);

            for (Test test : tests) {
                test.start();
            }

            long tSum = 0L;
            for (Test test : tests) {
                try {
                    test.join();
                    tSum += test.t;
                    if (test.throwable != null)
                        throw new RuntimeException(test.throwable);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            double tAvg = (double) tSum / tests.length;
            double vSum = 0L;
            for (int i = 0; i < tests.length; i++) {
                vSum += ((double) tests[i].t - tAvg) * ((double) tests[i].t - tAvg);
            }
            double v = vSum / tests.length;
            double σ = Math.sqrt(v);
            double tp = (double) loops * tests.length / tAvg;

            if (prevTp == 0d) prevTp = tp;

            double referenceTp;
            if (reference) {
                referenceTp = tp;
                try (OutputStream out = new FileOutputStream(testClass.getSimpleName() + "." + threads + ".reftp")) {
                    out.write(Long.toUnsignedString(Double.doubleToLongBits(referenceTp)).getBytes());
                }
            } else {
                try (InputStream in = new FileInputStream(testClass.getSimpleName() + "." + threads + ".reftp")) {
                    byte[] bytes = new byte[32];
                    int len = in.read(bytes);
                    referenceTp = Double.longBitsToDouble(Long.parseUnsignedLong(new String(bytes, 0, len)));
                }
            }

            System.out.println(
                String.format(
                    "%30s: %3d threads * %9d loops each: tAvg = %,15.3f ms, σ = %,10.3f ms, throughput = %,8.1f loops/ms (x %6.2f, reference x %6.2f)",
                    tests[0].getClass().getSimpleName(),
                    tests.length,
                    loops,
                    tAvg / 1000000d,
                    σ / 1000000d,
                    tp * 1000000d,
                    tp / prevTp,
                    tp / referenceTp
                )
            );

            return tp;
        }
        catch (NoSuchMethodException | InvocationTargetException |
            InstantiationException | IllegalAccessException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        boolean reference = args.length > 0 && args[0].equals("reference");
        double tp;
        System.out.println("warm-up:\n");
        tp = runTest(TestBulkDeclared.class, 1, 500000, 0d, reference);
        runTest(TestBulkDeclared.class, 1, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 1, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 1, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 1, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestBulkPublic.class, 1, 500000, 0d, reference);
        runTest(TestBulkPublic.class, 1, 500000, tp, reference);
        runTest(TestBulkPublic.class, 1, 500000, tp, reference);
        runTest(TestBulkPublic.class, 1, 500000, tp, reference);
        runTest(TestBulkPublic.class, 1, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestByNameDeclared.class, 1, 500000, 0d, reference);
        runTest(TestByNameDeclared.class, 1, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 1, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 1, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 1, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestByNamePublic.class, 1, 500000, 0d, reference);
        runTest(TestByNamePublic.class, 1, 500000, tp, reference);
        runTest(TestByNamePublic.class, 1, 500000, tp, reference);
        runTest(TestByNamePublic.class, 1, 500000, tp, reference);
        runTest(TestByNamePublic.class, 1, 500000, tp, reference);
        System.out.println();

        System.out.println("measure:\n");
        tp = runTest(TestBulkDeclared.class, 1, 500000, 0d, reference);
        runTest(TestBulkDeclared.class, 2, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 4, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 8, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 16, 500000, tp, reference);
        runTest(TestBulkDeclared.class, 32, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestBulkPublic.class, 1, 500000, 0d, reference);
        runTest(TestBulkPublic.class, 2, 500000, tp, reference);
        runTest(TestBulkPublic.class, 4, 500000, tp, reference);
        runTest(TestBulkPublic.class, 8, 500000, tp, reference);
        runTest(TestBulkPublic.class, 16, 500000, tp, reference);
        runTest(TestBulkPublic.class, 32, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestByNameDeclared.class, 1, 500000, 0d, reference);
        runTest(TestByNameDeclared.class, 2, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 4, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 8, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 16, 500000, tp, reference);
        runTest(TestByNameDeclared.class, 32, 500000, tp, reference);
        System.out.println();
        tp = runTest(TestByNamePublic.class, 1, 500000, 0d, reference);
        runTest(TestByNamePublic.class, 2, 500000, tp, reference);
        runTest(TestByNamePublic.class, 4, 500000, tp, reference);
        runTest(TestByNamePublic.class, 8, 500000, tp, reference);
        runTest(TestByNamePublic.class, 16, 500000, tp, reference);
        runTest(TestByNamePublic.class, 32, 500000, tp, reference);
        System.out.println();
    }
}
