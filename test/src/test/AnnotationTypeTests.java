package test;

import si.pele.microbench.TestRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static test.AnnotationTypeTests.*;

/**
 */
@Ann0()
@Ann1()
@Ann2()
@Ann3()
@Ann4()
@Ann5()
@Ann6()
@Ann7()
public class AnnotationTypeTests extends TestRunner
{

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann0 {
        String value() default "0";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann1 {
        String value() default "1";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2 {
        String value() default "2";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann3 {
        String value() default "3";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann4 {
        String value() default "4";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann5 {
        String value() default "5";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann6 {
        String value() default "6";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann7 {
        String value() default "7";
    }

    static final Annotation[] anns = AnnotationTypeTests.class.getDeclaredAnnotations();

    static final Ann0 ann0 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann0.class);
    static final Ann1 ann1 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann1.class);
    static final Ann2 ann2 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann2.class);
    static final Ann3 ann3 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann3.class);
    static final Ann4 ann4 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann4.class);
    static final Ann5 ann5 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann5.class);
    static final Ann6 ann6 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann6.class);
    static final Ann7 ann7 = AnnotationTypeTests.class.getDeclaredAnnotation(Ann7.class);

    public static class Ann_annotationType extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(ann0.annotationType());
            consume(ann1.annotationType());
            consume(ann2.annotationType());
            consume(ann3.annotationType());
            consume(ann4.annotationType());
            consume(ann5.annotationType());
            consume(ann6.annotationType());
            consume(ann7.annotationType());
        }
    }

    public static class Ann_value extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(ann0.value());
            consume(ann1.value());
            consume(ann2.value());
            consume(ann3.value());
            consume(ann4.value());
            consume(ann5.value());
            consume(ann6.value());
            consume(ann7.value());
        }
    }

    public static class Ann_hashCode extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(ann0.hashCode());
            consume(ann1.hashCode());
            consume(ann2.hashCode());
            consume(ann3.hashCode());
            consume(ann4.hashCode());
            consume(ann5.hashCode());
            consume(ann6.hashCode());
            consume(ann7.hashCode());
        }
    }

    public static class Ann_equals extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(ann0.equals(ann0));
            consume(ann1.equals(ann1));
            consume(ann2.equals(ann3));
            consume(ann3.equals(ann2));
            consume(ann4.equals(ann4));
            consume(ann5.equals(ann5));
            consume(ann6.equals(ann7));
            consume(ann7.equals(ann6));
        }
    }

    static void doTest(Class<? extends Test> testClass, long runDurationMillis, int minThreads, int maxThreads, int stepThreads) throws InterruptedException {
        System.out.println("Warm up:");
        System.out.println(runTest(testClass, runDurationMillis, minThreads));
        System.out.println(runTest(testClass, runDurationMillis, minThreads));

        System.out.println("Measure:");
        for (int threads = minThreads; threads <= maxThreads; threads+=stepThreads) {
            System.out.println(runTest(testClass, runDurationMillis, threads));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
//        doTest(Ann_annotationType.class, 5000L, 1, cpus, 1);
//        doTest(Ann_value.class, 5000L, 1, cpus, 1);
//        doTest(Ann_hashCode.class, 5000L, 1, cpus, 1);
        doTest(Ann_equals.class, 5000L, 1, cpus, 1);
    }
}
