/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;
import sun.reflect.MHMethodAccessor;
import sun.reflect.MethodAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Method;

/**
 * @author peter
 */
public class MHMAPerfTest extends TestRunner {

    public static String testMethod(String o) {
        return o;
    }

    static final Method testMethod;
    static {
        try {
            testMethod = MHMAPerfTest.class.getDeclaredMethod("testMethod", String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static class directInvocation extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            String arg = "BBB+";
            while (loop.nextIteration()) {
                devNull1.yield(testMethod(arg));
            }
        }
    }

    public static class generatedAccessorInvocation extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            try {
                MethodAccessor ma = ReflectionFactory.getReflectionFactory().newMethodAccessor(testMethod);
                Object[] args = new Object[] { "BBB+" };
                while (loop.nextIteration()) {
                    devNull1.yield(ma.invoke(null, args));
                }
            } catch (Exception e) {}
        }
    }

    public static class mhAccessorInvocation extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            try {
                MethodAccessor ma = new MHMethodAccessor(testMethod);
                Object[] args = new Object[] { "BBB+" };
                while (loop.nextIteration()) {
                    devNull1.yield(ma.invoke(null, args));
                }
            } catch (Exception e) {}
        }
    }

    public static void main(String[] args) throws Exception {
        doTest(mhAccessorInvocation.class, 2000L, 1, 4, 1);
        doTest(directInvocation.class, 2000L, 1, 4, 1);
        doTest(generatedAccessorInvocation.class, 2000L, 1, 4, 1);
    }
}
