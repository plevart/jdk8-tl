/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.misc.Unsafe;

import java.lang.invoke.IntFieldHandles;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.IntConsumer;

import static java.lang.invoke.IntFieldHandles.intFieldHandles;

/**
 * Test performance of various implementation approaches to instance field atomic operations
 */
public class IntFieldHandlesTest {
    // a volatile field "x"
    volatile int x = 4321;

    // MHs for atomic operations on field "x"
    static final IntFieldHandles X = intFieldHandles("x");

    // classic AtomicIntegerFieldUpdater for operations on field "x"
    static final AtomicIntegerFieldUpdater<IntFieldHandlesTest> X_FieldUpdater =
        AtomicIntegerFieldUpdater.newUpdater(IntFieldHandlesTest.class, "x");

    // special MH based AtomicIntegerFieldUpdater for operations on field "x" with constant MHs (a subclass per field)
    static final AtomicIntegerFieldUpdater<IntFieldHandlesTest> X_FieldUpdaterMH = new XUpdater();

    static class XUpdater extends AtomicIntegerFieldUpdaterMH<IntFieldHandlesTest> {
        private static final MethodHandle getVolatile = makeGeneric(X.getVolatile);
        private static final MethodHandle setVolatile = makeGeneric(X.setVolatile);
        private static final MethodHandle setOrdered = makeGeneric(X.setOrdered);
        private static final MethodHandle getAndSet = makeGeneric(X.getAndSet);
        private static final MethodHandle getAndAdd = makeGeneric(X.getAndAdd);
        private static final MethodHandle compareAndSet = makeGeneric(X.compareAndSet);

        MethodHandle getVolatileMH() { return getVolatile; }
        MethodHandle setVolatileMH() { return setVolatile; }
        MethodHandle setOrderedMH() { return setOrdered; }
        MethodHandle getAndSetMH() { return getAndSet; }
        MethodHandle getAndAddMH() { return getAndAdd; }
        MethodHandle compareAndSetMH() { return compareAndSet; }
    }

    // Unsafe access to field "x"
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

    void fieldUpdaterMhGet(int n) {
        for (int i = 0; i < n; i++) {
            if (X_FieldUpdaterMH.get(this) != 4321)
                throw new RuntimeException();
        }
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

    void fieldUpdaterMhSet(int n) {
        for (int i = 0; i < n; i++) {
            X_FieldUpdaterMH.set(this, 4321);
        }
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

    void fieldUpdaterMhSetGet(int n) {
        for (int i = 0; i < n; i++) {
            X_FieldUpdaterMH.set(this, 4321);
            if (X_FieldUpdaterMH.get(this) != 4321)
                throw new RuntimeException();
        }
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

    // Java bytecode volatile get followed by compare-and-set loop

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

    void fieldUpdaterMhGetCas(int n) {
        for (int i = 0; i < n; i++) {
            int x = this.x;
            while (!X_FieldUpdaterMH.compareAndSet(this, x, 4321)) {
                x = this.x;
            }
        }
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
        System.out.printf("%28s: %12d ns (%6.2f ns/op)\n", name, nanos, nsPerOp);
    }

    public static void main(String[] args) throws Throwable {

        IntFieldHandlesTest t = new IntFieldHandlesTest();

        try {
            System.out.println((int) X.getVolatile.invokeExact((IntFieldHandlesTest) null));
        } catch (NullPointerException expected) {
            System.out.println("Expected exception: " + expected);
        }

        System.out.println("\nvolatile get...");
        doTest("Java bytecode", t::javaGet);
        doTest("Unsafe", t::unsafeGet);
        doTest("IntFieldHandles", t::methodHandleGet);
        doTest("AtomicIntegerFieldUpdaterMH", t::fieldUpdaterMhGet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterGet);

        System.out.println("\nvolatile set...");
        doTest("Java bytecode", t::javaSet);
        doTest("Unsafe", t::unsafeSet);
        doTest("IntFieldHandles", t::methodHandleSet);
        doTest("AtomicIntegerFieldUpdaterMH", t::fieldUpdaterMhSet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterSet);

        System.out.println("\nvolatile set followed by volatile get...");
        doTest("Java bytecode", t::javaSetGet);
        doTest("Unsafe", t::unsafeSetGet);
        doTest("IntFieldHandles", t::methodHandleSetGet);
        doTest("AtomicIntegerFieldUpdaterMH", t::fieldUpdaterMhSetGet);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterSetGet);

        System.out.println("\nJava bytecode volatile get followed by compare-and-set loop...");
        doTest("Unsafe", t::unsafeGetCas);
        doTest("IntFieldHandles", t::methodHandleGetCas);
        doTest("AtomicIntegerFieldUpdaterMH", t::fieldUpdaterMhGetCas);
        doTest("AtomicIntegerFieldUpdater", t::fieldUpdaterGetCas);
    }
}
