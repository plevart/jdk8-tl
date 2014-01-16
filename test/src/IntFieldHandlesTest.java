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
    volatile int x;

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

    public static void main(String[] args) throws Throwable {

        IntFieldHandlesTest t = new IntFieldHandlesTest();

        System.out.println("            Java bytecode: " + t.javaSet(1000_000_000));
        System.out.println("            Java bytecode: " + t.javaSet(1000_000_000));
        System.out.println("            Java bytecode: " + t.javaSet(1000_000_000));
        System.out.println("                   Unsafe: " + t.unsafeSet(1000_000_000));
        System.out.println("                   Unsafe: " + t.unsafeSet(1000_000_000));
        System.out.println("                   Unsafe: " + t.unsafeSet(1000_000_000));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(1000_000_000));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(1000_000_000));
        System.out.println("          IntFieldHandles: " + t.methodHandleSet(1000_000_000));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(1000_000_000));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(1000_000_000));
        System.out.println("AtomicIntegerFieldUpdater: " + t.fieldUpdaterSet(1000_000_000));
    }
}
