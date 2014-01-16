/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.misc.Unsafe;

import java.lang.invoke.IntFieldHandles;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.lang.invoke.IntFieldHandles.fieldHandles;

/**
 * @author peter.levart@gmail.com
 */
public class IntFieldHandlesTest {
    volatile int x = 4321;

    static final IntFieldHandles X = fieldHandles("x");

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
    
    long javaGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            if (this.x != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    long methodHandleGet(int n) {
        try {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                if ((int) X.getVolatile.invokeExact(this) != 4321) throw new RuntimeException();
            }
            return System.nanoTime() - t0;
        } catch (Throwable ignore) { return 0L; }
    }

    long fieldUpdaterGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            if (X_FieldUpdater.get(this) != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    long unsafeGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            if (U.getIntVolatile(this, X_offset) != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    // volatile set

    long javaSet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            this.x = 4321;
        }
        return System.nanoTime() - t0;
    }

    long methodHandleSet(int n) {
        try {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                X.setVolatile.invokeExact(this, 4321);
            }
            return System.nanoTime() - t0;
        } catch (Throwable ignore) { return 0L; }
    }

    long fieldUpdaterSet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            X_FieldUpdater.set(this, 4321);
        }
        return System.nanoTime() - t0;
    }

    long unsafeSet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            U.putIntVolatile(this, X_offset, 4321);
        }
        return System.nanoTime() - t0;
    }

    // volatile set followed by get

    long javaSetGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            this.x = 4321;
            if (this.x != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    long methodHandleSetGet(int n) {
        try {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                X.setVolatile.invokeExact(this, 4321);
                if ((int) X.getVolatile.invokeExact(this) != 4321) throw new RuntimeException();
            }
            return System.nanoTime() - t0;
        } catch (Throwable ignore) { return 0L; }
    }

    long fieldUpdaterSetGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            X_FieldUpdater.set(this, 4321);
            if (X_FieldUpdater.get(this) != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    long unsafeSetGet(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            U.putIntVolatile(this, X_offset, 4321);
            if (U.getIntVolatile(this, X_offset) != 4321) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    // CAS

    long methodHandleCas(int n) {
        try {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                if (!(boolean) X.compareAndSet.invokeExact(this, 4321, 4321)) throw new RuntimeException();
            }
            return System.nanoTime() - t0;
        } catch (Throwable ignore) { return 0L; }
    }

    long fieldUpdaterCas(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            X_FieldUpdater.set(this, 4321);
            if (!X_FieldUpdater.compareAndSet(this, 4321, 4321)) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    long unsafeCas(int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            if (!U.compareAndSwapInt(this, X_offset, 4321, 4321)) throw new RuntimeException();
        }
        return System.nanoTime() - t0;
    }

    public static void main(String[] args) throws Throwable {

        IntFieldHandlesTest t = new IntFieldHandlesTest();

//        System.out.println((int)X.getVolatile.invokeExact((IntFieldHandlesTest) null));

        int n = 1000_000_000;

        System.out.println("\nvolatile get...");
        System.out.println("            Java bytecode: " + t.javaGet(n));
        System.out.println("            Java bytecode: " + t.javaGet(n));
        System.out.println("            Java bytecode: " + t.javaGet(n));
        System.out.println("                   Unsafe: " + t.unsafeGet(n));
        System.out.println("                   Unsafe: " + t.unsafeGet(n));
        System.out.println("                   Unsafe: " + t.unsafeGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterGet(n));

        System.out.println("\nvolatile set...");
        System.out.println("            Java bytecode: " + t.javaSet(n));
        System.out.println("            Java bytecode: " + t.javaSet(n));
        System.out.println("            Java bytecode: " + t.javaSet(n));
        System.out.println("                   Unsafe: " + t.unsafeSet(n));
        System.out.println("                   Unsafe: " + t.unsafeSet(n));
        System.out.println("                   Unsafe: " + t.unsafeSet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(n));

        System.out.println("\nvolatile set followed by volatile get...");
        System.out.println("            Java bytecode: " + t.javaSetGet(n));
        System.out.println("            Java bytecode: " + t.javaSetGet(n));
        System.out.println("            Java bytecode: " + t.javaSetGet(n));
        System.out.println("                   Unsafe: " + t.unsafeSetGet(n));
        System.out.println("                   Unsafe: " + t.unsafeSetGet(n));
        System.out.println("                   Unsafe: " + t.unsafeSetGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSetGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSetGet(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleSetGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSetGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSetGet(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSetGet(n));

        System.out.println("\ncompare and set...");
        System.out.println("                   Unsafe: " + t.unsafeCas(n));
        System.out.println("                   Unsafe: " + t.unsafeCas(n));
        System.out.println("                   Unsafe: " + t.unsafeCas(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleCas(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleCas(n));
        System.out.println("          IntFieldHandles: " + t.methodHandleCas(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterCas(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterCas(n));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterCas(n));
    }
}
