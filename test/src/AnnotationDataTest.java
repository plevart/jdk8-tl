/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author peter
 */
@AnnotationDataTest.Ann1
public class AnnotationDataTest extends TestRunner {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann1 {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2 {}

    public static class getAnnotationWhenPresent extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(AnnotationDataTest.class.getAnnotation(Ann1.class));
            }
        }
    }

    public static class getAnnotationWhenAbsent extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(AnnotationDataTest.class.getAnnotation(Ann2.class));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        doTest(getAnnotationWhenPresent.class, 5000L, 1, 4, 1);
        doTest(getAnnotationWhenAbsent.class, 5000L, 1, 4, 1);
    }
}
