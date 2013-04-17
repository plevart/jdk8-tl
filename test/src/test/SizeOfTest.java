package test;

import si.pele.microbench.SizeOf;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 */
public class SizeOfTest {

    public interface I0 {}

    public interface I1 {}

    public interface I2 {}

    public interface I3 {}

    public interface I4 {}

    public interface I5 {}

    public interface I6 {}

    public interface I7 {}

    public interface I8 {}

    public interface I9 {}

    static final Class<?>[] interfaces = {I0.class, I1.class, I2.class, I3.class, I4.class, I5.class, I6.class, I7.class, I8.class, I9.class};

    static void doTest(Object... caches) {
        ClassLoader[] classLoaders = {
            new ClassLoader(Thread.currentThread().getContextClassLoader()) {},
            new ClassLoader(Thread.currentThread().getContextClassLoader()) {},
        };
        Class<?>[] proxyClasses = new Class[interfaces.length * classLoaders.length];
        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);
        System.out.printf("class     proxy     size of   delta to\n");
        System.out.printf("loaders   classes   caches    prev.ln.\n");
        System.out.printf("--------  --------  --------  --------\n");
        long size = 0L;
        for (Object cache : caches)
            size += sizeOf.deepSizeOf(cache);
        System.out.printf("%8d  %8d  %8d  %8d\n", 0, 0, size, size);
        long prevSize = size;
        int classes = 0;
        for (int cli = 0; cli < classLoaders.length; cli++) {
            for (int ii = 0; ii < interfaces.length; ii++) {
                proxyClasses[classes++] = Proxy.getProxyClass(classLoaders[cli], interfaces[ii]);
                size = 0L;
                for (Object cache : caches)
                    size += sizeOf.deepSizeOf(cache);
                System.out.printf("%8d  %8d  %8d  %8d\n", cli+1, classes, size, size - prevSize);
                prevSize = size;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (true) {
            Field cacheField = Proxy.class.getDeclaredField("proxyClassCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);
            doTest(cache);
        } else {
            Field cacheField = Proxy.class.getDeclaredField("loaderToCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);
            Field cacheField2 = Proxy.class.getDeclaredField("proxyClasses");
            cacheField2.setAccessible(true);
            Object cache2 = cacheField2.get(null);
            doTest(cache, cache2);
        }
    }
}
