package test;

import si.pele.microbench.TestRunner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static test.AnnotationEqualsTest.*;

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
public class AnnotationEqualsTest extends TestRunner {
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

    static final Ann0 ann0 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann0.class);
    static final Ann1 ann1 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann1.class);
    static final Ann2 ann2 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann2.class);
    static final Ann3 ann3 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann3.class);
    static final Ann4 ann4 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann4.class);
    static final Ann5 ann5 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann5.class);
    static final Ann6 ann6 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann6.class);
    static final Ann7 ann7 = AnnotationEqualsTest.class.getDeclaredAnnotation(Ann7.class);

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

        @Override
        protected void checkConsumeCounts(long ops, long defaultValuesConsumed, long nonDefaultValuesConsumed) {
            assert defaultValuesConsumed == ops * 4L && nonDefaultValuesConsumed == ops * 4L;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
        doTest(Ann_equals.class, 5000L, 1, cpus, 1);
    }
}
