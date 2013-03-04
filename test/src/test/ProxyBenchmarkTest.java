package test;

import si.pele.microbench.TestRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Proxy;

import static test.ProxyBenchmarkTest.Ann1;
import static test.ProxyBenchmarkTest.Ann2;
import static test.ProxyBenchmarkTest.Ann3;
import static test.ProxyBenchmarkTest.Ann4;
import static test.ProxyBenchmarkTest.Ann5;

/**
 */
@Ann1()
@Ann2()
@Ann3()
@Ann4()
@Ann5()
public class ProxyBenchmarkTest extends TestRunner {

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

    static final Annotation[] anns = ProxyBenchmarkTest.class.getDeclaredAnnotations();
    static final Annotation ann1 = anns[0];
    static final Annotation ann2 = anns[1];
    static final Annotation ann3 = anns[2];
    static final Annotation ann4 = anns[3];
    static final Annotation ann5 = anns[4];

    public static class Proxy_getProxyClass extends TestRunner.Test {
        private static final ClassLoader cl = Ann1.class.getClassLoader();

        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(Proxy.getProxyClass(cl, Ann1.class));
                devNull2.yield(Proxy.getProxyClass(cl, Ann2.class));
                devNull3.yield(Proxy.getProxyClass(cl, Ann3.class));
                devNull4.yield(Proxy.getProxyClass(cl, Ann4.class));
                devNull5.yield(Proxy.getProxyClass(cl, Ann5.class));

            }
        }
    }

    public static class Proxy_isProxyClassTrue extends TestRunner.Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(Proxy.isProxyClass(ann1.getClass()));
                devNull2.yield(Proxy.isProxyClass(ann2.getClass()));
                devNull3.yield(Proxy.isProxyClass(ann3.getClass()));
                devNull4.yield(Proxy.isProxyClass(ann4.getClass()));
                devNull5.yield(Proxy.isProxyClass(ann5.getClass()));

            }
        }
    }

    public static class Proxy_isProxyClassFalse extends TestRunner.Test {

        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(Proxy.isProxyClass(Ann1.class));
                devNull2.yield(Proxy.isProxyClass(Ann2.class));
                devNull3.yield(Proxy.isProxyClass(Ann3.class));
                devNull4.yield(Proxy.isProxyClass(Ann4.class));
                devNull5.yield(Proxy.isProxyClass(Ann5.class));

            }
        }
    }

    public static class Annotation_equals extends TestRunner.Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(ann1.equals(ann1));
                devNull2.yield(ann2.equals(ann3));
                devNull3.yield(ann3.equals(ann2));
                devNull4.yield(ann4.equals(ann4));
                devNull5.yield(ann5.equals(ann5));
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int maxThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        doTest(Proxy_getProxyClass.class, 5000L, 1, maxThreads, 1);
        doTest(Proxy_isProxyClassTrue.class, 5000L, 1, maxThreads, 1);
        doTest(Proxy_isProxyClassFalse.class, 5000L, 1, maxThreads, 1);
        doTest(Annotation_equals.class, 5000L, 1, maxThreads, 1);
    }
}
