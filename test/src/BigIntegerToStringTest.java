/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author peter
 */
public class BigIntegerToStringTest {

    static void assertEquals(String s1, String s2) {
        if (!Objects.equals(s1, s2))
            throw new AssertionError(
                "Strings are not equal:\n" +
                "string1: " + s1 + "\n" +
                "string2: " + s2
            );
    }

    static void test(String prefix, String append) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 1000; i++) {
            String bigNumber = sb.toString();
            BigInteger bi = new BigInteger(bigNumber);
            assertEquals(bigNumber, bi.toString(10));
            sb.append(append);
        }
    }

    public static void main(String[] args) {
        test("1", "0");
        test("1", "1");
        test("9", "0");
        test("9", "9");
    }
}
