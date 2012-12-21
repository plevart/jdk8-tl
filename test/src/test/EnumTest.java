package test;

import java.util.EnumSet;
import java.util.Objects;

/**
 */
public class EnumTest {
    static enum E1 { C1, C2 }
    static enum E2 { C1, C2 }
    static enum E3 { C1, C2 }
    static enum E4 { C1, C2 }
    static enum E5 { C1, C2 }

    static void assertTrue(boolean value) {
        if (!value) throw new AssertionError();
    }

    static long testMyEnumValueOf() {
        long t0 = System.nanoTime();
        for (int i = 0; i < 30_000_000; i++) {
            assertTrue(E1.valueOf("C1") == E1.C1);
            assertTrue(E1.valueOf("C2") == E1.C2);
            assertTrue(E2.valueOf("C1") == E2.C1);
            assertTrue(E2.valueOf("C2") == E2.C2);
            assertTrue(E3.valueOf("C1") == E3.C1);
            assertTrue(E3.valueOf("C2") == E3.C2);
            assertTrue(E4.valueOf("C1") == E4.C1);
            assertTrue(E4.valueOf("C2") == E4.C2);
            assertTrue(E5.valueOf("C1") == E5.C1);
            assertTrue(E5.valueOf("C2") == E5.C2);
        }
        return System.nanoTime() - t0;
    }

    static void testMyEnumValueOf8x() {
        System.out.print("XEnum.valueOf(String): ");
        for (int i = 0; i < 8; i++)
            System.out.print(testMyEnumValueOf() + " ");
        System.out.println();
    }

    static long testEnumSetNoneOf() {
        long t0 = System.nanoTime();
        for (int i = 0; i < 300_000_000; i++) {
            Objects.requireNonNull(EnumSet.noneOf(E1.class));
            Objects.requireNonNull(EnumSet.noneOf(E2.class));
            Objects.requireNonNull(EnumSet.noneOf(E3.class));
            Objects.requireNonNull(EnumSet.noneOf(E4.class));
            Objects.requireNonNull(EnumSet.noneOf(E5.class));
        }
        return System.nanoTime() - t0;
    }

    static void testEnumSetNoneOf8x() {
        System.out.print("EnumSet.noneOf(Class): ");
        for (int i = 0; i < 8; i++)
            System.out.print(testEnumSetNoneOf() + " ");
        System.out.println();

        try {
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(1500L);
        }
        catch (InterruptedException e) {}
    }

    public static void main(String[] args) {
        testEnumSetNoneOf8x();
        testMyEnumValueOf8x();
        testEnumSetNoneOf8x();
    }
}
