package test;

import java.util.EnumSet;
import java.util.Objects;

/**
 */
public class EnumTest
{
    static enum MyEnum
    {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN
    }

    static void assertTrue(boolean value)
    {
        if (!value) throw new AssertionError();
    }

    static long testMyEnumValueOf()
    {
        long t0 = System.nanoTime();
        for (int i = 0; i < 30_000_000; i++)
        {
            assertTrue(MyEnum.valueOf("ONE") == MyEnum.ONE);
            assertTrue(MyEnum.valueOf("TWO") == MyEnum.TWO);
            assertTrue(MyEnum.valueOf("THREE") == MyEnum.THREE);
            assertTrue(MyEnum.valueOf("FOUR") == MyEnum.FOUR);
            assertTrue(MyEnum.valueOf("FIVE") == MyEnum.FIVE);
            assertTrue(MyEnum.valueOf("SIX") == MyEnum.SIX);
            assertTrue(MyEnum.valueOf("SEVEN") == MyEnum.SEVEN);
            assertTrue(MyEnum.valueOf("EIGHT") == MyEnum.EIGHT);
            assertTrue(MyEnum.valueOf("NINE") == MyEnum.NINE);
            assertTrue(MyEnum.valueOf("TEN") == MyEnum.TEN);
        }
        return System.nanoTime() - t0;
    }

    static void testMyEnumValueOf8x()
    {
        System.out.print("     MyEnum.valueOf(String): ");
        for (int i = 0; i < 8; i++)
            System.out.print(testMyEnumValueOf() + " ");
        System.out.println();
    }

    static long testEnumSetNoneOf()
    {
        long t0 = System.nanoTime();
        for (int i = 0; i < 300_000_000; i++)
        {
            Objects.requireNonNull(EnumSet.noneOf(MyEnum.class));
        }
        return System.nanoTime() - t0;
    }

    static void testEnumSetNoneOf8x()
    {
        System.out.print("      EnumSet.noneOf(Class): ");
        for (int i = 0; i < 8; i++)
            System.out.print(testEnumSetNoneOf() + " ");
        System.out.println();

        try
        {
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(1500L);
        }
        catch (InterruptedException e) {}
    }

    public static void main(String[] args)
    {
        testEnumSetNoneOf8x();
        testMyEnumValueOf8x();
        testEnumSetNoneOf8x();
    }
}
