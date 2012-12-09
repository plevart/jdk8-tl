package test;

import sun.reflect.annotation.AnnotationType;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ReflectionTest {
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface InheritedAnn {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann1 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann3 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann4 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann5 {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonexistentAnn {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Recursive0
    @Inherited
    public @interface Recursive0 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @RecursiveB
    @Inherited
    public @interface RecursiveA {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @RecursiveA
    @Inherited
    public @interface RecursiveB {
    }

    @Recursive0
    @RecursiveA
    @RecursiveB
    public static class Class0 {}

    @InheritedAnn("A")
    @Ann1("A")
    @Ann2("A")
    @Ann3("A")
    @Ann4("A")
    @Ann5("A")
    public static class ClassA extends Class0 {
        @Ann1("A.f1")
        @Ann2("A.f1")
        @Ann3("A.f1")
        @Ann4("A.f1")
        @Ann5("A.f1")
        public String f1;

        @Ann1("A.<init>")
        @Ann2("A.<init>")
        @Ann3("A.<init>")
        @Ann4("A.<init>")
        @Ann5("A.<init>")
        public ClassA() {
        }

        @Ann1("A.m1")
        @Ann2("A.m1")
        @Ann3("A.m1")
        @Ann4("A.m1")
        @Ann5("A.m1")
        public void m1() {
        }

        @Ann1("A.m2")
        @Ann2("A.m2")
        @Ann3("A.m2")
        @Ann4("A.m2")
        @Ann5("A.m2")
        public void m2() {
        }
    }

    @Ann1("B")
    @Ann2("B")
    @Ann3("B")
    @Ann4("B")
    @Ann5("B")
    public static class ClassB extends ClassA {
        @Ann1("B.f1")
        @Ann2("B.f1")
        @Ann3("B.f1")
        @Ann4("B.f1")
        @Ann5("B.f1")
        public String f1;

        @Ann1("B.<init>")
        @Ann2("B.<init>")
        @Ann3("B.<init>")
        @Ann4("B.<init>")
        @Ann5("B.<init>")
        public ClassB() {
        }

        @Ann1("B.m1")
        @Ann2("B.m1")
        @Ann3("B.m1")
        @Ann4("B.m1")
        @Ann5("B.m1")
        public void m1() {
        }

        @Ann1("B.m2")
        @Ann2("B.m2")
        @Ann3("B.m2")
        @Ann4("B.m2")
        @Ann5("B.m2")
        public void m2() {
        }
    }

    @Ann1("C")
    @Ann2("C")
    @Ann3("C")
    @Ann4("C")
    @Ann5("C")
    public static class ClassC extends ClassB {
        @Ann1("C.f1")
        @Ann2("C.f1")
        @Ann3("C.f1")
        @Ann4("C.f1")
        @Ann5("C.f1")
        public String f1;

        @Ann1("C.<init>")
        @Ann2("C.<init>")
        @Ann3("C.<init>")
        @Ann4("C.<init>")
        @Ann5("C.<init>")
        public ClassC() {
        }

        @Ann1("C.m1")
        @Ann2("C.m1")
        @Ann3("C.m1")
        @Ann4("C.m1")
        @Ann5("C.m1")
        public void m1() {
        }

        @Ann1("C.m2")
        @Ann2("C.m2")
        @Ann3("C.m2")
        @Ann4("C.m2")
        @Ann5("C.m2")
        public void m2() {
        }
    }

    static final Object NOT_ANNOTATION = new Object();

    static final Set<String> SKIPPED_METHODS = new HashSet<>(Arrays.asList("wait", "notify", "notifyAll", "toString", "equals", "hashCode", "getClass"));

    static Method[] filterMethods(Method[] methods) {
        int n = 0;
        for (Method m : methods)
            if (!SKIPPED_METHODS.contains(m.getName()))
                n++;
        Method[] filteredMethods = new Method[n];
        n = 0;
        for (Method m : methods)
            if (!SKIPPED_METHODS.contains(m.getName()))
                filteredMethods[n++] = m;
        return filteredMethods;
    }

    static void assertAnnotationOrNull(Annotation ann) {
        if (ann == NOT_ANNOTATION) {
            throw new AssertionError("unexpected annotation instance");
        }
    }

    static void assertNull(Annotation ann) {
        if (ann != null) {
            throw new AssertionError("expected null");
        }
    }

    static void testExistentAnn(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods) {
        assertAnnotationOrNull(clazz.getAnnotation(InheritedAnn.class));
        assertAnnotationOrNull(clazz.getAnnotation(Ann1.class));
        assertAnnotationOrNull(clazz.getAnnotation(Ann2.class));
        assertAnnotationOrNull(clazz.getAnnotation(Ann3.class));
        assertAnnotationOrNull(clazz.getAnnotation(Ann4.class));
        assertAnnotationOrNull(clazz.getAnnotation(Ann5.class));

        for (Field f : fields) {
            assertAnnotationOrNull(f.getAnnotation(Ann1.class));
            assertAnnotationOrNull(f.getAnnotation(Ann2.class));
            assertAnnotationOrNull(f.getAnnotation(Ann3.class));
            assertAnnotationOrNull(f.getAnnotation(Ann4.class));
            assertAnnotationOrNull(f.getAnnotation(Ann5.class));
        }

        for (Constructor<?> c : constructors) {
            assertAnnotationOrNull(c.getAnnotation(Ann1.class));
            assertAnnotationOrNull(c.getAnnotation(Ann2.class));
            assertAnnotationOrNull(c.getAnnotation(Ann3.class));
            assertAnnotationOrNull(c.getAnnotation(Ann4.class));
            assertAnnotationOrNull(c.getAnnotation(Ann5.class));
        }

        for (Method m : methods) {
            assertAnnotationOrNull(m.getAnnotation(Ann1.class));
            assertAnnotationOrNull(m.getAnnotation(Ann2.class));
            assertAnnotationOrNull(m.getAnnotation(Ann3.class));
            assertAnnotationOrNull(m.getAnnotation(Ann4.class));
            assertAnnotationOrNull(m.getAnnotation(Ann5.class));
        }
    }

    static void testNonexistentAnn(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods) {
        assertNull(clazz.getAnnotation(NonexistentAnn.class));

        for (Field f : fields) {
            assertNull(f.getAnnotation(NonexistentAnn.class));
        }


        for (Constructor<?> c : constructors) {
            assertNull(c.getAnnotation(NonexistentAnn.class));
        }

        for (Method m : methods) {
            assertNull(m.getAnnotation(NonexistentAnn.class));
        }
    }

    static void testBulkAnn(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods) {

        clazz.getAnnotations();

        for (Field f : fields) {
            f.getAnnotations();
        }

        for (Constructor<?> c : constructors) {
            c.getAnnotations();
        }

        for (Method m : methods) {
            m.getAnnotations();
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

    static class TestAnnotationRootCache extends Test {
        public TestAnnotationRootCache(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            for (int i = 0; i < loops; i++) {
                testExistentAnn(ClassA.class, ClassA.class.getFields(), ClassA.class.getConstructors(), ClassA.class.getMethods());
                testExistentAnn(ClassB.class, ClassB.class.getFields(), ClassB.class.getConstructors(), ClassB.class.getMethods());
                testExistentAnn(ClassC.class, ClassC.class.getFields(), ClassC.class.getConstructors(), ClassC.class.getMethods());
            }
        }
    }

    static class TestAnnotationExistent extends Test {
        public TestAnnotationExistent(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            Field[] classAfields = ClassA.class.getFields();
            Constructor[] classAconstructors = ClassA.class.getConstructors();
            Method[] classAmethods = filterMethods(ClassA.class.getMethods());

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = filterMethods(ClassB.class.getMethods());

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = filterMethods(ClassC.class.getMethods());

            for (int i = 0; i < loops; i++) {
                testExistentAnn(ClassA.class, classAfields, classAconstructors, classAmethods);
                testExistentAnn(ClassB.class, classBfields, classBconstructors, classBmethods);
                testExistentAnn(ClassC.class, classCfields, classCconstructors, classCmethods);
            }
        }
    }

    static class TestAnnotationNonexistent extends Test {
        public TestAnnotationNonexistent(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            Field[] classAfields = ClassA.class.getFields();
            Constructor[] classAconstructors = ClassA.class.getConstructors();
            Method[] classAmethods = filterMethods(ClassA.class.getMethods());

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = filterMethods(ClassB.class.getMethods());

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = filterMethods(ClassC.class.getMethods());

            for (int i = 0; i < loops; i++) {
                testNonexistentAnn(ClassA.class, classAfields, classAconstructors, classAmethods);
                testNonexistentAnn(ClassB.class, classBfields, classBconstructors, classBmethods);
                testNonexistentAnn(ClassC.class, classCfields, classCconstructors, classCmethods);
            }
        }
    }

    static class TestAnnotationBulk extends Test {
        public TestAnnotationBulk(int loops) {
            super(loops);
        }

        @Override
        protected void runTest() {
            Field[] classAfields = ClassA.class.getFields();
            Constructor[] classAconstructors = ClassA.class.getConstructors();
            Method[] classAmethods = filterMethods(ClassA.class.getMethods());

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = filterMethods(ClassB.class.getMethods());

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = filterMethods(ClassC.class.getMethods());

            for (int i = 0; i < loops; i++) {
                testBulkAnn(ClassA.class, classAfields, classAconstructors, classAmethods);
                testBulkAnn(ClassB.class, classBfields, classBconstructors, classBmethods);
                testBulkAnn(ClassC.class, classCfields, classCconstructors, classCmethods);
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
            }
            else {
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
        tp = runTest(TestAnnotationRootCache.class, 1, 10000, 0d, reference);
        runTest(TestAnnotationRootCache.class, 1, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 1, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 1, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 1, 10000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationExistent.class, 1, 1000000, 0d, reference);
        runTest(TestAnnotationExistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 1, 1000000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationNonexistent.class, 1, 1000000, 0d, reference);
        runTest(TestAnnotationNonexistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 1, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 1, 1000000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationBulk.class, 1, 100000, 0d, reference);
        runTest(TestAnnotationBulk.class, 1, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 1, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 1, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 1, 100000, tp, reference);
        System.out.println();

        System.out.println("measure:\n");
        tp = runTest(TestAnnotationRootCache.class, 1, 10000, 0d, reference);
        runTest(TestAnnotationRootCache.class, 2, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 4, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 8, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 16, 10000, tp, reference);
        runTest(TestAnnotationRootCache.class, 32, 10000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationExistent.class, 1, 1000000, 0d, reference);
        runTest(TestAnnotationExistent.class, 2, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 4, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 8, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 16, 1000000, tp, reference);
        runTest(TestAnnotationExistent.class, 32, 1000000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationNonexistent.class, 1, 1000000, 0d, reference);
        runTest(TestAnnotationNonexistent.class, 2, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 4, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 8, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 16, 1000000, tp, reference);
        runTest(TestAnnotationNonexistent.class, 32, 1000000, tp, reference);
        System.out.println();
        tp = runTest(TestAnnotationBulk.class, 1, 100000, 0d, reference);
        runTest(TestAnnotationBulk.class, 2, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 4, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 8, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 16, 100000, tp, reference);
        runTest(TestAnnotationBulk.class, 32, 100000, tp, reference);
        System.out.println();
    }

    public static class Dump {

        static StringBuilder dump(Annotation ann, StringBuilder sb) throws Exception {
            Class<? extends Annotation> annotationType = ann.annotationType();
            sb.append("@").append(annotationType.getSimpleName()).append("(");
            boolean first = true;
            for (Map.Entry<String, Method> e : AnnotationType.getInstance(annotationType).members().entrySet()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(e.getKey()).append("=").append(e.getValue().invoke(ann));
            }
            sb.append(")");
            return sb;
        }

        static StringBuilder dump(Annotation[] anns, String prefix, StringBuilder sb) throws Exception {
            Arrays.sort(
                anns, new Comparator<Annotation>() {
                @Override
                public int compare(Annotation a1, Annotation a2) {
                    return a1.annotationType().getName().compareTo(a2.annotationType().getName());
                }
            }
            );
            for (Annotation ann : anns) {
                dump(ann, sb.append(prefix)).append("\n");
            }
            return sb;
        }

        static StringBuilder dump(Class<?> clazz, StringBuilder sb) throws Exception {
            dump(clazz.getAnnotations(), "", sb.append("\n"));
            sb.append("class ").append(clazz.getSimpleName()).append(" {\n");
            for (Field f : clazz.getFields()) {
                dump(f.getAnnotations(), "    ", sb.append("\n"));
                sb.append("    ").append(f.toGenericString()).append(";\n");
            }
            for (Constructor c : clazz.getConstructors()) {
                dump(c.getAnnotations(), "    ", sb.append("\n"));
                sb.append("    ").append(c.toGenericString()).append(";\n");
            }
            for (Method m : filterMethods(clazz.getMethods())) {
                dump(m.getAnnotations(), "    ", sb.append("\n"));
                sb.append("    ").append(m.toGenericString()).append(";\n");
            }
            return sb.append("}\n");
        }

        public static void main(String[] args) {
            try {
                StringBuilder sb = new StringBuilder();
                dump(ClassA.class, sb);
                dump(ClassB.class, sb);
                dump(ClassC.class, sb);
                System.out.println(sb);
            }
            catch (Throwable e) {
                e.printStackTrace(System.out);
            }
        }
    }
}
