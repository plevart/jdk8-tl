package test;

import sun.reflect.annotation.AnnotationType;

import java.io.IOException;
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

    static void test(Annotation ann) {
        if (ann == NOT_ANNOTATION) {
            throw new AssertionError();
        }
    }

    static void testExistentAnn(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods) {
        test(clazz.getAnnotation(InheritedAnn.class));
        test(clazz.getAnnotation(Ann1.class));
        test(clazz.getAnnotation(Ann2.class));
        test(clazz.getAnnotation(Ann3.class));
        test(clazz.getAnnotation(Ann4.class));
        test(clazz.getAnnotation(Ann5.class));

        for (Field f : fields) {
            test(f.getAnnotation(Ann1.class));
            test(f.getAnnotation(Ann2.class));
            test(f.getAnnotation(Ann3.class));
            test(f.getAnnotation(Ann4.class));
            test(f.getAnnotation(Ann5.class));
        }

        for (Constructor<?> c : constructors) {
            test(c.getAnnotation(Ann1.class));
            test(c.getAnnotation(Ann2.class));
            test(c.getAnnotation(Ann3.class));
            test(c.getAnnotation(Ann4.class));
            test(c.getAnnotation(Ann5.class));
        }

        for (Method m : methods) {
            test(m.getAnnotation(Ann1.class));
            test(m.getAnnotation(Ann2.class));
            test(m.getAnnotation(Ann3.class));
            test(m.getAnnotation(Ann4.class));
            test(m.getAnnotation(Ann5.class));
        }
    }

    static void testNonexistentAnn(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods) {
        test(clazz.getAnnotation(NonexistentAnn.class));

        for (Field f : fields) {
            test(f.getAnnotation(NonexistentAnn.class));
        }


        for (Constructor<?> c : constructors) {
            test(c.getAnnotation(NonexistentAnn.class));
        }

        for (Method m : methods) {
            test(m.getAnnotation(NonexistentAnn.class));
        }
    }

    static abstract class Test extends Thread {
        final int loops;

        protected Test(int loops) {
            this.loops = loops;
        }

        protected abstract void runTest();

        long t;
        Throwable throwable;

        @Override
        public void run() {
            long t0 = System.nanoTime();
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
            Method[] classAmethods = ClassA.class.getMethods();

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = ClassB.class.getMethods();

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = ClassC.class.getMethods();

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
            Method[] classAmethods = ClassA.class.getMethods();

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = ClassB.class.getMethods();

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = ClassC.class.getMethods();

            for (int i = 0; i < loops; i++) {
                testNonexistentAnn(ClassA.class, classAfields, classAconstructors, classAmethods);
                testNonexistentAnn(ClassB.class, classBfields, classBconstructors, classBmethods);
                testNonexistentAnn(ClassC.class, classCfields, classCconstructors, classCmethods);
            }
        }
    }

    static double runTest(Class<? extends Test> testClass, int threads, int loops, double prevT) {

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

            for (int i = 0; i < tests.length; i++) {
                tests[i].start();
            }

            long tSum = 0L;
            for (int i = 0; i < tests.length; i++) {
                try {
                    tests[i].join();
                    tSum += tests[i].t;
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

            if (prevT == 0d) prevT = tAvg;

            System.out.println(
                String.format(
                    "%30s: %3d threads * %9d loops each: tAvg = %,15.3f ms (x %6.2f), σ = %,10.3f ms",
                    tests[0].getClass().getSimpleName(),
                    tests.length,
                    loops,
                    tAvg / 1000000d,
                    tAvg / prevT,
                    σ / 1000000d
                )
            );

            return tAvg;
        }
        catch (NoSuchMethodException | InvocationTargetException |
            InstantiationException | IllegalAccessException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        double t;
        System.out.println("warm-up:\n");
        t = runTest(TestAnnotationRootCache.class, 1, 10000, 0d);
        runTest(TestAnnotationRootCache.class, 1, 10000, t);
        runTest(TestAnnotationRootCache.class, 1, 10000, t);
        runTest(TestAnnotationRootCache.class, 1, 10000, t);
        runTest(TestAnnotationRootCache.class, 1, 10000, t);
        System.out.println();
        t = runTest(TestAnnotationExistent.class, 1, 100000, 0d);
        runTest(TestAnnotationExistent.class, 1, 100000, t);
        runTest(TestAnnotationExistent.class, 1, 100000, t);
        runTest(TestAnnotationExistent.class, 1, 100000, t);
        runTest(TestAnnotationExistent.class, 1, 100000, t);
        System.out.println();
        t = runTest(TestAnnotationNonexistent.class, 1, 100000, 0d);
        runTest(TestAnnotationNonexistent.class, 1, 100000, t);
        runTest(TestAnnotationNonexistent.class, 1, 100000, t);
        runTest(TestAnnotationNonexistent.class, 1, 100000, t);
        runTest(TestAnnotationNonexistent.class, 1, 100000, t);
        System.out.println();

        System.out.println("measure:\n");
        t = runTest(TestAnnotationRootCache.class, 1, 10000, 0d);
        runTest(TestAnnotationRootCache.class, 2, 10000, t);
        runTest(TestAnnotationRootCache.class, 4, 10000, t);
        runTest(TestAnnotationRootCache.class, 8, 10000, t);
        runTest(TestAnnotationRootCache.class, 32, 10000, t);
        runTest(TestAnnotationRootCache.class, 128, 10000, t);
        System.out.println();
        t = runTest(TestAnnotationExistent.class, 1, 100000, 0d);
        runTest(TestAnnotationExistent.class, 2, 100000, t);
        runTest(TestAnnotationExistent.class, 4, 100000, t);
        runTest(TestAnnotationExistent.class, 8, 100000, t);
        runTest(TestAnnotationExistent.class, 32, 100000, t);
        runTest(TestAnnotationExistent.class, 128, 100000, t);
        System.out.println();
        t = runTest(TestAnnotationNonexistent.class, 1, 100000, 0d);
        runTest(TestAnnotationNonexistent.class, 2, 100000, t);
        runTest(TestAnnotationNonexistent.class, 4, 100000, t);
        runTest(TestAnnotationNonexistent.class, 8, 100000, t);
        runTest(TestAnnotationNonexistent.class, 32, 100000, t);
        runTest(TestAnnotationNonexistent.class, 128, 100000, t);
        System.out.println();
    }

    public static class Dump {

        static final Set<String> skippedMethods = new HashSet<>(Arrays.asList("wait", "notify", "notifyAll", "toString", "equals", "hashCode", "getClass"));

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
            for (Method m : clazz.getMethods()) {
                if (!skippedMethods.contains(m.getName())) {
                    dump(m.getAnnotations(), "    ", sb.append("\n"));
                    sb.append("    ").append(m.toGenericString()).append(";\n");
                }
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
