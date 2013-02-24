package test;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: peter
 * Date: 2/24/13
 * Time: 5:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpTest {

    static long add(long a, long b) {
        int aLo = (int) a;
        int bLo = (int) b;

        int sumLo = aLo + bLo;

        int aHi = (int) (a >>> 32);
        int bHi = (int) ((b + ((long)aLo & 0xFFFFFFFFL)) >>> 32);

        int sumHi = aHi + bHi;

        return (((long) sumHi) << 32) | (((long) sumLo) & 0xFFFFFFFFL);
    }

    public static void main(String[] args) {
        Random rnd = new Random();
        for (int i = 0; i < 10000000; i++) {
            long a = rnd.nextLong();
            long b = rnd.nextLong();
            if (a+b != add(a, b)) {
                System.out.println((a+b) + " != " + add(a, b));
            }
        }
    }
}
