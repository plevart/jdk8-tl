/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package java.lang.reflect;

import java.util.Arrays;
import java.util.function.BiFunction;

/**
 * @author peter
 */
public class WeakCacheTest {

    static void doGc() {
        try {
            System.gc();
            Thread.sleep(200L);
            System.gc();
            Thread.sleep(200L);
            System.gc();
            Thread.sleep(200L);
        }
        catch (InterruptedException e) {}
    }

    static void doTest(WeakCache<String, Integer, String> wc) {
        String a = new String("a");
        String b = new String("b");

        String a1 = wc.get(a, 1);
        String a2 = wc.get(a, 2);
        String b1 = wc.get(b, 1);
        String b2 = wc.get(b, 2);

        System.out.println(Arrays.asList(a1, a2, b1, b2));

        System.out.println(wc.containsValue(a1));
        System.out.println(wc.containsValue(a2));
        System.out.println(wc.containsValue(b1));
        System.out.println(wc.containsValue(b2));
        System.out.println(wc.size());

        doGc();

        System.out.println(wc.containsValue(a1));
        System.out.println(wc.containsValue(a2));
        System.out.println(wc.containsValue(b1));
        System.out.println(wc.containsValue(b2));
        System.out.println(wc.size());
    }

    public static void main(String[] args) {
        WeakCache<String, Integer, String> fwc = new WeakCache<>(
            new BiFunction<String, Integer, Object>() {
                @Override
                public Object apply(String s, Integer integer) {
                    return integer;
                }
            },
            new BiFunction<String, Integer, String>() {
                @Override
                public String apply(String s, Integer integer) {
                    return s + ":" + integer;
                }
            }
        );

        doTest(fwc);
        doGc();
        System.out.println(fwc.size());
    }
}
