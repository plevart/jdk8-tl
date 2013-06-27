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
public class BigIntegerPowerCache2 {

    private static class Node {
        final BigInteger value;
        Node next;
        Node(BigInteger value) { this.value = value; }
    }

    private static volatile Node[][] powerCache;

    static {
        powerCache = new Node[Character.MAX_RADIX + 1][];
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            powerCache[i] = new Node[]{new Node(BigInteger.valueOf(i))};
        }
    }

    private static BigInteger getRadixConversionCache(int radix, int exponent) {
        Node[] cacheLine = powerCache[radix]; // volatile read
        if (exponent < cacheLine.length)
            return cacheLine[exponent].value;

        int oldLength = cacheLine.length;
        cacheLine = Arrays.copyOf(cacheLine, exponent + 1);
        Node prevNode = cacheLine[oldLength - 1];
        for (int i = oldLength; i <= exponent; i++) {
            Node node;
            synchronized (prevNode) {
                node = prevNode.next;
                if (node == null) {
                    node = new Node(prevNode.value.pow(2));
                    prevNode.next = node;
                }
            }
            cacheLine[i] = prevNode = node;
        }

        Node[][] pc = powerCache; // volatile read again
        if (exponent >= pc[radix].length) {
            pc = pc.clone();
            pc[radix] = cacheLine;
            powerCache = pc; // volatile write, publish
        }
        return cacheLine[exponent].value;
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
