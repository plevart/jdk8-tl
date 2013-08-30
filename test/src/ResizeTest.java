/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
public class ResizeTest {

    static void dump(Iterator<Integer> i) {
        List<Integer> l = new ArrayList<>();
        while (i.hasNext()) {
            l.add(i.next());
        }
        Collections.sort(l);
        System.out.println(l.size() + ": " + l + "; " + i);
    }

    public static void main(String[] args) {
        ConcurrentMap<Integer, Integer> map = new ConcurrentHashMap<>(1);
        List<Iterator<Integer>> iterators = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            map.put(i, i);
            iterators.add(map.values().iterator());
        }

        for (Iterator<Integer> i : iterators) {
            dump(i);
        }
    }
}
