package test;

import si.pele.microbench.TestRunner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Proxy;

import static test.ProxyBenchmarkTest.*;

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
public class ProxyBenchmarkTest extends TestRunner {
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

    static final Ann0 ann0 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann0.class);
    static final Ann1 ann1 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann1.class);
    static final Ann2 ann2 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann2.class);
    static final Ann3 ann3 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann3.class);
    static final Ann4 ann4 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann4.class);
    static final Ann5 ann5 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann5.class);
    static final Ann6 ann6 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann6.class);
    static final Ann7 ann7 = ProxyBenchmarkTest.class.getDeclaredAnnotation(Ann7.class);

    public static class Proxy_getProxyClass extends TestRunner.Test {
        private static final ClassLoader cl = Ann0.class.getClassLoader();
        @Override
        protected void doOp() {
            consume(Proxy.getProxyClass(cl, Ann0.class));
            consume(Proxy.getProxyClass(cl, Ann1.class));
            consume(Proxy.getProxyClass(cl, Ann2.class));
            consume(Proxy.getProxyClass(cl, Ann3.class));
            consume(Proxy.getProxyClass(cl, Ann4.class));
            consume(Proxy.getProxyClass(cl, Ann5.class));
            consume(Proxy.getProxyClass(cl, Ann6.class));
            consume(Proxy.getProxyClass(cl, Ann7.class));
        }

        @Override
        protected void checkConsumeCounts(long ops, long defaultValuesConsumed, long nonDefaultValuesConsumed) {
            if (!(defaultValuesConsumed == 0 && nonDefaultValuesConsumed == ops * 8L))
                throw new AssertionError();
        }
    }

    public static class Proxy_isProxyClassTrue extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(Proxy.isProxyClass(ann0.getClass()));
            consume(Proxy.isProxyClass(ann1.getClass()));
            consume(Proxy.isProxyClass(ann2.getClass()));
            consume(Proxy.isProxyClass(ann3.getClass()));
            consume(Proxy.isProxyClass(ann4.getClass()));
            consume(Proxy.isProxyClass(ann5.getClass()));
            consume(Proxy.isProxyClass(ann6.getClass()));
            consume(Proxy.isProxyClass(ann7.getClass()));
        }

        @Override
        protected void checkConsumeCounts(long ops, long defaultValuesConsumed, long nonDefaultValuesConsumed) {
            if (!(defaultValuesConsumed == 0 && nonDefaultValuesConsumed == ops * 8L))
                throw new AssertionError();
        }
    }

    public static class Proxy_isProxyClassFalse extends TestRunner.Test {
        @Override
        protected void doOp() {
            consume(Proxy.isProxyClass(Ann0.class));
            consume(Proxy.isProxyClass(Ann1.class));
            consume(Proxy.isProxyClass(Ann2.class));
            consume(Proxy.isProxyClass(Ann3.class));
            consume(Proxy.isProxyClass(Ann4.class));
            consume(Proxy.isProxyClass(Ann5.class));
            consume(Proxy.isProxyClass(Ann6.class));
            consume(Proxy.isProxyClass(Ann7.class));
        }

        @Override
        protected void checkConsumeCounts(long ops, long defaultValuesConsumed, long nonDefaultValuesConsumed) {
            if (!(defaultValuesConsumed == ops * 8L && nonDefaultValuesConsumed == 0L))
                throw new AssertionError();
        }
    }


    public static class Annotation_equals extends TestRunner.Test {
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
            if (!(defaultValuesConsumed == ops * 4L && nonDefaultValuesConsumed == ops * 4L))
                throw new AssertionError();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
        doTest(Proxy_getProxyClass.class, 5000L, 1, cpus, 1);
        doTest(Proxy_isProxyClassTrue.class, 5000L, 1, cpus, 1);
        doTest(Proxy_isProxyClassFalse.class, 5000L, 1, cpus, 1);
        doTest(Annotation_equals.class, 5000L, 1, cpus, 1);
    }
}
