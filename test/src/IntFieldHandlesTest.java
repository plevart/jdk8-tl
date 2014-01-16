/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.misc.Unsafe;

import java.lang.invoke.IntFieldHandles;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.IntConsumer;

import static java.lang.invoke.IntFieldHandles.intFieldHandles;

public class IntFieldHandlesTest {
    volatile int x = 4321;

    static final IntFieldHandles X = intFieldHandles("x");

    static final AtomicIntegerFieldUpdater<IntFieldHandlesTest> X_FieldUpdater =
        AtomicIntegerFieldUpdater.newUpdater(IntFieldHandlesTest.class, "x");

    static final Unsafe U;
    static final long X_offset;

    static {
        try {
            Field U_Field = Unsafe.class.getDeclaredField("theUnsafe");
            U_Field.setAccessible(true);
            U = (Unsafe) U_Field.get(null);
            X_offset = U.objectFieldOffset(IntFieldHandlesTest.class.getDeclaredField("x"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // volatile get

    void javaGet(int n) {
        for (int i = 0; i < n; i++) {
            if (this.x != 4321)
                throw new RuntimeException();
        }
    }

    void methodHandleGet(int n) {
        try {
            for (int i = 0; i < n; i++) {
                if ((int) X.getVolatile.invokeExact(this) != 4321)
                    throw new RuntimeException();
            }
        } catch (Throwable ignore) {}
    }

    void fieldUpdaterGet(int n) {
        for (int i = 0; i < n; i++) {
            if (X_FieldUpdater.get(this) != 4321)
                throw new RuntimeException();
        }
    }

    void unsafeGet(int n) {
        for (int i = 0; i < n; i++) {
            if (U.getIntVolatile(this, X_offset) != 4321)
                throw new RuntimeException();
        }
    }

    // volatile set

    void javaSet(int n) {
        for (int i = 0; i < n; i++) {
            this.x = 4321;
        }
    }

    void methodHandleSet(int n) {
        try {
            for (int i = 0; i < n; i++) {
                X.setVolatile.invokeExact(this, 4321);
            }
        } catch (Throwable ignore) {}
    }

    void fieldUpdaterSet(int n) {
        for (int i = 0; i < n; i++) {
            X_FieldUpdater.set(this, 4321);
        }
    }

    void unsafeSet(int n) {
        for (int i = 0; i < n; i++) {
            U.putIntVolatile(this, X_offset, 4321);
        }
    }

    // volatile set followed by volatile get

    void javaSetGet(int n) {
        for (int i = 0; i < n; i++) {
            this.x = 4321;
            if (this.x != 4321)
                throw new RuntimeException();
        }
    }

    void methodHandleSetGet(int n) {
        try {
            for (int i = 0; i < n; i++) {
                X.setVolatile.invokeExact(this, 4321);
                if ((int) X.getVolatile.invokeExact(this) != 4321)
                    throw new RuntimeException();
            }
        } catch (Throwable ignore) {}
    }

    void fieldUpdaterSetGet(int n) {
        for (int i = 0; i < n; i++) {
            X_FieldUpdater.set(this, 4321);
            if (X_FieldUpdater.get(this) != 4321)
                throw new RuntimeException();
        }
    }

    void unsafeSetGet(int n) {
        for (int i = 0; i < n; i++) {
            U.putIntVolatile(this, X_offset, 4321);
            if (U.getIntVolatile(this, X_offset) != 4321)
                throw new RuntimeException();
        }
    }

    // Java bytecode volatile get followed by compare-and-set

    void methodHandleGetCas(int n) {
        try {
            for (int i = 0; i < n; i++) {
                int x = this.x;
                while (!(boolean) X.compareAndSet.invokeExact(this, x, 4321)) {
                    x = this.x;
                }
            }
        } catch (Throwable ignore) {}
    }

    void fieldUpdaterGetCas(int n) {
        for (int i = 0; i < n; i++) {
            int x = this.x;
            while (!X_FieldUpdater.compareAndSet(this, x, 4321)) {
                x = this.x;
            }
        }
    }

    void unsafeGetCas(int n) {
        for (int i = 0; i < n; i++) {
            int x = this.x;
            while (!U.compareAndSwapInt(this, X_offset, x, 4321)) {
                x = this.x;
            }
        }
    }

    // test runner
    static void doTest(String name, IntConsumer test) {
        int n = 1_000_000_000;
        // double run for warming-up
        test.accept(n * 2);
        // measure
        long t0 = System.nanoTime();
        test.accept(n);
        long nanos = System.nanoTime() - t0;
        double nsPerOp = (double) nanos / (double) n;
        System.out.printf("%26s: %12d ns (%6.2f ns/op)\n", name, nanos, nsPerOp);
    }

    public static void main(String[] args) throws Throwable {

        IntFieldHandlesTest t = new IntFieldHandlesTest();

        try {
            System.out.println((int) X.getVolatile.invokeExact((IntFieldHandlesTest) null));
        } catch (NullPointerException expected) {
            System.out.println("Expected exception: " + expected);
        }

        System.out.println("\nvolatile get...");
        doTest("            Java bytecode", t::javaGet);
        doTest("                   Unsafe", t::unsafeGet);
        doTest("          IntFieldHandles", t::methodHandleGet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterGet);

        System.out.println("\nvolatile set...");
        doTest("            Java bytecode", t::javaSet);
        doTest("                   Unsafe", t::unsafeSet);
        doTest("          IntFieldHandles", t::methodHandleSet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterSet);

        System.out.println("\nvolatile set followed by volatile get...");
        doTest("            Java bytecode", t::javaSetGet);
        doTest("                   Unsafe", t::unsafeSetGet);
        doTest("          IntFieldHandles", t::methodHandleSetGet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterSetGet);

        System.out.println("\nJava bytecode volatile get followed by compare-and-set...");
        doTest("                   Unsafe", t::unsafeGetCas);
        doTest("          IntFieldHandles", t::methodHandleGetCas);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterGetCas);
    }
}
