/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author peter
 */
public class AnnotationDispatchPerfTest extends TestRunner {

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(AnnCont.class)
    @interface Ann {
        int v1();
        int v2();
        int v3();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnCont {
        Ann[] value();
    }

    @Ann(v1 = 1, v2 = 2, v3 = 3)
    static class C {}

    @Ann(v1 = 1, v2 = 2, v3 = 3)
    static class D {}

    @Ann(v1 = 1, v2 = 2, v3 = 3)
    @Ann(v1 = 1, v2 = 2, v3 = 3)
    static class E {}

    @Ann(v1 = 1, v2 = 2, v3 = 3)
    @AnnCont(@Ann(v1 = 1, v2 = 2, v3 = 3))
    static class F {}

    // not-OneOfUs-kind of annotation implementation
    static final Ann annX = new Ann() {
        @Override public int v1() { return 1; }
        @Override public int v2() { return 2; }
        @Override public int v3() { return 3; }
        @Override public Class<? extends Annotation> annotationType() { return Ann.class; }
    };

    public static class Annotation_annotationType extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            Ann ann = C.class.getDeclaredAnnotation(Ann.class);
            while (loop.nextIteration()) {
                if (ann.annotationType() != Ann.class) throw new AssertionError();
            }
        }
    }

    public static class Annotation_value extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            Ann annC = C.class.getDeclaredAnnotation(Ann.class);
            while (loop.nextIteration()) {
                if (annC.v1() != 1) throw new AssertionError();
            }
        }
    }

    public static class Annotation_equals_oneOfUs extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            Ann ann1 = C.class.getDeclaredAnnotation(Ann.class);
            Ann ann2 = D.class.getDeclaredAnnotation(Ann.class);
            while (loop.nextIteration()) {
                if (!ann1.equals(ann2)) throw new AssertionError();
            }
        }
    }

    public static class Annotation_equals_notOneOfUs extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            Ann ann1 = C.class.getDeclaredAnnotation(Ann.class);
            Ann ann2 = annX;
            while (loop.nextIteration()) {
                if (!ann1.equals(ann2)) throw new AssertionError();
            }
        }
    }

    public static class Class_getDeclaredAnnotationsByType_direct extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(D.class.getDeclaredAnnotationsByType(Ann.class));
            }
        }
    }

    public static class Class_getDeclaredAnnotationsByType_indirect extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(E.class.getDeclaredAnnotationsByType(Ann.class));
            }
        }
    }

    public static class Class_getDeclaredAnnotationsByType_direct_indirect extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(F.class.getDeclaredAnnotationsByType(Ann.class));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        doTest(Annotation_annotationType.class, 2000L, 1, 8, 1);
        doTest(Annotation_value.class, 2000L, 1, 8, 1);
        doTest(Annotation_equals_oneOfUs.class, 2000L, 1, 8, 1);
        doTest(Annotation_equals_notOneOfUs.class, 2000L, 1, 8, 1);

        doTest(Class_getDeclaredAnnotationsByType_direct.class, 2000L, 1, 8, 1);
        doTest(Class_getDeclaredAnnotationsByType_indirect.class, 2000L, 1, 8, 1);
        doTest(Class_getDeclaredAnnotationsByType_direct_indirect.class, 2000L, 1, 8, 1);
    }
}
