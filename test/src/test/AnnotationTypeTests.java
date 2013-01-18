package test;

import sun.reflect.annotation.AnnotationMap;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static test.AnnotationTypeTests.*;

/**
 */
@Ann0
@Ann1
@Ann2
@Ann3
@Ann4
@Ann5
@Ann6
@Ann7
public class AnnotationTypeTests extends TestRunner {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann0 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann1 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann3 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann4 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann5 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann6 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann7 {}

    public static class AnnotationGetType extends TestRunner.Test {
        static final Annotation[] anns = AnnotationTypeTests.class.getDeclaredAnnotations();

        @Override
        protected void doOp() {
            for (Annotation ann : anns)
                consume(ann.annotationType());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Warm up:");
        System.out.println(runTest(AnnotationGetType.class, 5000L, 1));
        System.out.println(runTest(AnnotationGetType.class, 5000L, 1));

        System.out.println("Measure:");
        System.out.println(runTest(AnnotationGetType.class, 5000L, 1));
    }
}
