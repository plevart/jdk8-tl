package test;

/**
 */
public class EnumTest
{
    static enum MyEnum {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN
    }

    static void assertTrue(boolean value) {
        if (!value) throw new AssertionError();
    }

    static long test() {
        long t0 = System.nanoTime();
        for (int i = 0; i < 30000000; i++)
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

    static void test8x() {
        for (int i = 0; i < 8; i++)
            System.out.print(test() + " ");
        System.out.println();
    }

    public static void main(String[] args)
    {
        test8x();
    }
}
