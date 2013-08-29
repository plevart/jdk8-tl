/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
public class ResizeTest {
    public static void main(String[] args) {
        ConcurrentMap<Integer, Integer> map = new ConcurrentHashMap<>(1);
        for (int i = 0; i < 50; i++) {
            map.put(i, i);
            System.out.println(map.values());
        }
    }
}
