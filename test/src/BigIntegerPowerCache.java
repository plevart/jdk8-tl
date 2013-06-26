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

    private static final BigInteger[][] powerCache =
        new BigInteger[Character.MAX_RADIX + 1][];

    private static BigInteger getRadixConversionCache(
        int radix,
        int exponent
    ) {
        BigInteger[] cacheLine = powerCache[radix];
        int oldLength = cacheLine == null ? 0 : cacheLine.length;
        if (exponent >= oldLength) { // needs resizing/creation?
            // invariant: each cacheLine has length > 0
            if (oldLength == 0) { // creation
                cacheLine = new BigInteger[exponent + 1];
            }
            else { // resizing
                // increase by factor of 1.5 (like ArrayList)
                int newLength = oldLength + (oldLength >> 1);
                // if that's not enough, take exact needed length
                if (newLength <= exponent) newLength = exponent + 1;
                cacheLine = Arrays.copyOf(cacheLine, newLength);
            }
            powerCache[radix] = cacheLine; // install new cacheLine
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

    public static void main(String[] args) {
        for (int i = 4; i >= 0; i--) {
            System.out.println(i + ": " + getRadixConversionCache(2, i));
        }
        for (int i = 0; i < 5; i++) {
            System.out.println(i + ": " + getRadixConversionCache(3, i));
        }
    }
}
