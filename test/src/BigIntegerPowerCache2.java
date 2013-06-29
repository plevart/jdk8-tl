/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.math.BigInteger;

/**
 * @author peter
 */
public class BigIntegerPowerCache2 {

    private static class Node {
        final BigInteger value;
        Node next;

        Node(BigInteger value) { this.value = value; }
    }

    private static final Node[] powerCache;
    private static final BigInteger[][] powerCacheIndex;
    private static final int POWER_CACHE_LINE_CHUNK = 16;

    static {
        powerCache = new Node[Character.MAX_RADIX + 1];
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            powerCache[i] = new Node(BigInteger.valueOf(i));
        }
        powerCacheIndex = new BigInteger[Character.MAX_RADIX + 1][];
    }

    static BigInteger getRadixConversionCache(int radix, int exponent) {
        BigInteger[] cacheLine = powerCacheIndex[radix];
        if (cacheLine != null && exponent < cacheLine.length) { // cache line is long enough
            BigInteger value = cacheLine[exponent];
            if (value != null) {
                return value;
            }
            return fillCacheLine(cacheLine, powerCache[radix], exponent);
        } else { // we need to extend / create cache line
            cacheLine = new BigInteger[(exponent / POWER_CACHE_LINE_CHUNK + 1) * POWER_CACHE_LINE_CHUNK];
            BigInteger result = fillCacheLine(cacheLine, powerCache[radix], exponent);
            powerCacheIndex[radix] = cacheLine; // install new line
            return result;
        }
    }

    private static BigInteger fillCacheLine(BigInteger[] cacheLine, Node node, int exponent) {
        cacheLine[0] = node.value;
        for (int i = 1; i <= exponent; i++) {
            // not-broken (JDK5+) double-checked locking
            Node nextNode = node.next;
            if (nextNode == null) {
                synchronized (node) {
                    nextNode = node.next;
                    if (nextNode == null) {
                        node.next = nextNode = new Node(node.value.pow(2));
                    }
                }
            }
            node = nextNode;
            cacheLine[i] = node.value;
        }
        return node.value;
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
