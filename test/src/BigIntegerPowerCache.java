/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author peter
 */
public class BigIntegerPowerCache {

    private static final BigInteger[][] powerCacheMy =
        new BigInteger[Character.MAX_RADIX + 1][];

    private static BigInteger getRadixConversionCacheMy(
        int radix,
        int exponent
    ) {
        BigInteger[] cacheLine = powerCacheMy[radix];
        int oldLength = cacheLine == null ? 0 : cacheLine.length;
        if (exponent >= oldLength) { // needs resizing/creation?
            // invariant: each cacheLine has length > 0
            if (oldLength == 0) { // creation
                cacheLine = new BigInteger[exponent + 1];
            } else { // resizing
                // increase by factor of 1.5 (like ArrayList)
                int newLength = oldLength + (oldLength >> 1);
                // if that's not enough, take exact needed length
                if (newLength <= exponent) newLength = exponent + 1;
                cacheLine = Arrays.copyOf(cacheLine, newLength);
            }
            powerCacheMy[radix] = cacheLine; // install new cacheLine
        }
        // search for 1st non-null power from min(oldLength - 1, exponent) backwards
        int s;
        BigInteger power = null;
        for (s = Math.min(oldLength - 1, exponent); s >= 0; s--) {
            power = cacheLine[s];
            if (power != null) break;
        }
        // calculate the rest up to and including exponent
        for (int i = s + 1; i <= exponent; i++) {
            power = power == null ? BigInteger.valueOf(radix) : power.pow(2);
            cacheLine[i] = power;
        }
        return power;
    }


    private static volatile BigInteger[][] powerCache;

    static {
        /*
         * Initialize the cache of radix^(2^x) values used for base conversion
         * with just the very first value.  Additional values will be created
         * on demand.
         */
        powerCache = new BigInteger[Character.MAX_RADIX + 1][];

        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            powerCache[i] = new BigInteger[]{BigInteger.valueOf(i)};
        }
    }

    private static BigInteger getRadixConversionCache(int radix, int exponent) {
        BigInteger[] cacheLine = powerCache[radix]; // volatile read
        if (exponent < cacheLine.length)
            return cacheLine[exponent];

        int oldLength = cacheLine.length;
        cacheLine = Arrays.copyOf(cacheLine, exponent + 1);
        for (int i = oldLength; i <= exponent; i++)
            cacheLine[i] = cacheLine[i - 1].pow(2);

        BigInteger[][] pc = powerCache; // volatile read again
        if (exponent >= pc[radix].length) {
            pc = pc.clone();
            pc[radix] = cacheLine;
            powerCache = pc; // volatile write, publish
        }
        return cacheLine[exponent];
    }


    public static void main(String[] args) {
        for (int i = 4; i >= 0; i--) {
            System.out.println(i + ": " + getRadixConversionCache(2, i));
        }
        for (int i = 0; i < 5; i++) {
            System.out.println(i + ": " + getRadixConversionCache(3, i));
        }
    }
}
