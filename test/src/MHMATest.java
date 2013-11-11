/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.reflect.MHMethodAccessor;
import sun.reflect.MethodAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author peter
 */
public class MHMATest {

    public static void pubStatVoid(int i) {}

    public static int pubStatInt(int i) { return i; }

    public void pubInstVoid(int i) {}

    public int pubInstInt(int i) { return i; }

    private static void privStatVoid(int i) {}

    private static int privStatInt(int i) { return i; }

    private void privInstVoid(int i) {}

    private int privInstInt(int i) { return i; }


    static void doTest(MethodAccessor ma, Object target, Object[] args, Object expectedReturn, Throwable ...expectedExceptions) {
        Object ret;
        Throwable exc;
        try {
            ret = ma.invoke(target, args);
            exc = null;
        }
        catch (Throwable e) {
            ret = null;
            exc = e;
        }

        if (exc != null) {
            boolean match = false;
            for (Throwable expected : expectedExceptions) {
                match |= expected.getClass().isInstance(exc) && Objects.equals(expected.getMessage(), exc.getMessage());
            }
            if (!match) {
                throw new RuntimeException("Expected exceptions: " + Arrays.toString(expectedExceptions) + " got: " + exc, exc);
            }
        } else if (expectedExceptions.length > 0) {
            throw new RuntimeException("Expected exceptions: " + Arrays.toString(expectedExceptions) + " got no exception");
        } else if (!Objects.equals(ret, expectedReturn)) {
            throw new RuntimeException("Expected return: " + expectedReturn + " got: " + ret);
        }
    }

    static void doTest(Method m, Object target, Object[] args, Object expectedReturn, Throwable ...expectedException) {
        MethodAccessor ma0 = ReflectionFactory.getReflectionFactory().newMethodAccessor(m);
        MethodAccessor ma1 = new MHMethodAccessor(m);
        try {
            doTest(ma0, target, args, expectedReturn, expectedException);
        } catch (Throwable e) {
            throw new RuntimeException("Default method accessor for: " + m + ": " + e.getMessage(), e);
        }
        try {
            doTest(ma1, target, args, expectedReturn, expectedException);
        } catch (Throwable e) {
            throw new RuntimeException("MH method accessor for: " + m + ": " + e.getMessage(), e);
        }
    }
    
    public static void main(String[] args) throws Exception {
        MHMATest inst = new MHMATest();
        Object wrongInst = new Object();

        doTest(MHMATest.class.getDeclaredMethod("pubStatVoid", int.class), null, new Object[] {12}, null);
        doTest(MHMATest.class.getDeclaredMethod("pubStatInt", int.class), null, new Object[] {12}, 12);
        doTest(MHMATest.class.getDeclaredMethod("pubInstVoid", int.class), inst, new Object[] {12}, null);
        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), inst, new Object[] {12}, 12);

        doTest(MHMATest.class.getDeclaredMethod("privStatVoid", int.class), null, new Object[] {12}, null);
        doTest(MHMATest.class.getDeclaredMethod("privStatInt", int.class), null, new Object[] {12}, 12);
        doTest(MHMATest.class.getDeclaredMethod("privInstVoid", int.class), inst, new Object[] {12}, null);
        doTest(MHMATest.class.getDeclaredMethod("privInstInt", int.class), inst, new Object[] {12}, 12);

        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), inst, new Object[] {"a"}, null, new IllegalArgumentException("argument type mismatch"), new IllegalArgumentException("target or argument type mismatch"));
        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), inst, new Object[] {12, 13}, null, new IllegalArgumentException("wrong number of arguments"), new IllegalArgumentException("array is not of length 1"));
        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), wrongInst, new Object[] {12}, 12, new IllegalArgumentException("object is not an instance of declaring class"), new IllegalArgumentException("target or argument type mismatch"));
        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), null, new Object[] {12}, 12, new NullPointerException());
        doTest(MHMATest.class.getDeclaredMethod("pubInstInt", int.class), inst, null, null, new IllegalArgumentException("wrong number of arguments"), new IllegalArgumentException("array is not of length 1"));

        System.out.println(MHMethodAccessor.getCaller());
        Method getCallerM = MHMethodAccessor.class.getDeclaredMethod("getCaller");
        System.out.println(getCallerM.invoke(null));
        MethodAccessor getCallerMA0 = ReflectionFactory.getReflectionFactory().newMethodAccessor(getCallerM);
        System.out.println(getCallerMA0.invoke(null, new Object[0]));
        MethodAccessor getCallerMA1 = new MHMethodAccessor(getCallerM);
        System.out.println(getCallerMA1.invoke(null, new Object[0]));
    }
}
