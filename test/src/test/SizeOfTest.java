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

//    public interface I8 {}
//
//    public interface I9 {}

    public interface M1 {}

    public interface M2 {}

    public interface M3 {}

    static final Class<?>[] interfaces = {I0.class, I1.class, I2.class, I3.class, I4.class, I5.class, I6.class, I7.class};

    static void doTest(boolean newProxy, int interfacesPerProxy, Object... caches) {
        ClassLoader[] classLoaders = {
            new ClassLoader(Thread.currentThread().getContextClassLoader()) {},
            new ClassLoader(Thread.currentThread().getContextClassLoader()) {},
        };
        Class<?>[] proxyClasses = new Class[interfaces.length * classLoaders.length];
        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);
//        SizeOf sizeOfOut = new SizeOf(SizeOf.Visitor.STDOUT);
        System.out.printf("--------------------------------------\n");
        System.out.printf("      %s\n", newProxy ? "Patched j.l.r.Proxy" : "Original j.l.r.Proxy");
        System.out.printf("      %d interfaces / proxy class\n", interfacesPerProxy);
        System.out.printf("\n");
        System.out.printf("class     proxy     size of   delta to\n");
        System.out.printf("loaders   classes   caches    prev.ln.\n");
        System.out.printf("--------  --------  --------  --------\n");
        long size = 0L;
        for (Object cache : caches)
            size += sizeOf.deepSizeOf(cache);
        System.out.printf("%8d  %8d  %8d  %8d\n", 0, 0, size, size);
        long prevSize = size;
        int classes = 0;
        Class<?>[][] intfcsSwitch = {
            new Class[]{ },
            new Class[]{ null },
            new Class[]{null, M1.class },
            new Class[]{null, M1.class, M2.class },
            new Class[]{null, M1.class, M2.class, M3.class }
        };
        for (int cli = 0; cli < classLoaders.length; cli++) {
            for (int ii = 0; ii < interfaces.length; ii++) {
                Class<?>[] intfcs = intfcsSwitch[interfacesPerProxy];
                if (interfacesPerProxy > 0) intfcs[0] = interfaces[ii];
                proxyClasses[classes++] = Proxy.getProxyClass(classLoaders[cli], intfcs);
                size = 0L;
                for (Object cache : caches)
                    size += sizeOf.deepSizeOf(cache);
                System.out.printf("%8d  %8d  %8d  %8d\n", cli+1, classes, size, size - prevSize);
                prevSize = size;
            }
        }
    }

    static void testCaches(boolean newProxy, int interfacesPerProxyClass) throws Exception {
        if (newProxy) {
            Field cacheField = Proxy.class.getDeclaredField("proxyClassCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);
            doTest(newProxy, interfacesPerProxyClass, cache);
        } else {
            Field cacheField = Proxy.class.getDeclaredField("loaderToCache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);
            Field cacheField2 = Proxy.class.getDeclaredField("proxyClasses");
            cacheField2.setAccessible(true);
            Object cache2 = cacheField2.get(null);
            doTest(newProxy, interfacesPerProxyClass, cache, cache2);
        }
    }

    public static void main(String[] args) throws Exception {
        testCaches(Boolean.parseBoolean(args[0]), Integer.parseInt(args[1]));
    }
}
