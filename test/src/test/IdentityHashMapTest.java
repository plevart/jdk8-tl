package test;

import java.util.*;

public class IdentityHashMapTest {
    static long testToArray(Collection<?> collection, Object[] array, int loops) {
        long t0 = System.nanoTime();
        for (int i = 0; i < loops; i++)
            collection.toArray(array);
        return System.nanoTime() - t0;
    }

    static void testToArray8x(String name, Collection<?> collection, Object[] array, int loops) {
        System.out.print(name);
        for (int i = 0; i < 8; i++)
            System.out.print(String.format(" %,15d", testToArray(collection, array, loops)));
        System.out.println();
    }

    static void test(int size) {
        HashMap<Object, Object> hashMap = new HashMap<>(size * 4 / 3);
        IdentityHashMap<Object, Object> identityHashMap = new IdentityHashMap<>(size);
        int loops = 100000000 / size;

        System.out.println("size=" + size + ", HashMap.capacity=" + hmCapacity(size * 4 / 3) + ", IdentityHashMap.capacity=" + ihmCapacity(size) + ", loops=" + loops);
        System.out.println();

        for (int i = 0; i < size; i++) {
            Object key = i == 0 ? null : new Object();
            Object value = new Object();
            hashMap.put(key, value);
            identityHashMap.put(key, value);
        }

        Object[] array = new Object[hashMap.size()];

        try {
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
        }
        catch (InterruptedException e) {}

        testToArray8x("          HashMap.keySet().toArray():", hashMap.keySet(), array, loops);
        testToArray8x("  IdentityHashMap.keySet().toArray():", identityHashMap.keySet(), array, loops);
        System.out.println();
        testToArray8x("          HashMap.values().toArray():", hashMap.values(), array, loops);
        testToArray8x("  IdentityHashMap.values().toArray():", identityHashMap.values(), array, loops);
        System.out.println();
        testToArray8x("        HashMap.entrySet().toArray():", hashMap.entrySet(), array, loops);
        testToArray8x("IdentityHashMap.entrySet().toArray():", identityHashMap.entrySet(), array, loops);

        System.out.println();
    }

    private static int hmCapacity(int initialCapacity) {
        int MAXIMUM_CAPACITY = 1 << 30;

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        return capacity;
    }

    private static int ihmCapacity(int expectedMaxSize) {
        int MINIMUM_CAPACITY = 4;
        int MAXIMUM_CAPACITY = 1 << 29;

        // Compute min capacity for expectedMaxSize given a load factor of 2/3
        int minCapacity = (3 * expectedMaxSize)/2;

        // Compute the appropriate capacity
        int result;
        if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
            result = MAXIMUM_CAPACITY;
        } else {
            result = MINIMUM_CAPACITY;
            while (result < minCapacity)
                result <<= 1;
        }
        return result;
    }

    public static void main(String[] args) {
        test(5);
        test(10);
        test(20);
        test(50);
        test(100);
        test(10000);
    }
}
