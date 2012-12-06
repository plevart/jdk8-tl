package util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 */
public class ConcurrentWeakValuesMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    private static final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg; tgn != null; tg = tgn, tgn = tg.getParent()) ;
        Thread cleaner = new Cleaner(tg, "ConcurrentWeakValuesMap Cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    static class Cleaner extends Thread {
        Cleaner(ThreadGroup group, String name) {
            super(group, name);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Reference<?> ref = refQueue.remove();
                    ((Evictable) ref).evict();
                }
                catch (InterruptedException e) {}
            }
        }
    }

    interface Evictable {
        void evict();
    }

    class ValueRef extends WeakReference<V> implements Evictable {
        final K key;

        ValueRef(K key, V value) {
            super(value, refQueue);
            this.key = key;
        }

        public void evict() {
            System.out.println("Evicting entry for key: " + key);
            backMap.remove(key, this);
        }
    }

    private final ConcurrentMap<K, ValueRef> backMap;

    public ConcurrentWeakValuesMap(int initialCapacity) {
        backMap = new ConcurrentHashMap<>(initialCapacity);
    }

    public ConcurrentWeakValuesMap(int initialCapacity, float loadFactor) {
        backMap = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    public ConcurrentWeakValuesMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        backMap = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    public ConcurrentWeakValuesMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / 0.75f) + 1, 16), 0.75f, 16);
        putAll(m);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int size() {
                return backMap.size();
            }
        };
    }

    @Override
    public int size() {
        return backMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        ValueRef ref = backMap.get(key);
        return ref != null && ref.get() != null;
    }

    @Override
    public V get(Object key) {
        ValueRef ref = backMap.get(key);
        return ref != null ? ref.get() : null;
    }

    @Override
    public V put(K key, V value) {
        // we don't support null keys or values
        if (key == null || value == null) throw new NullPointerException("null keys or values not supported");
        ValueRef ref = backMap.put(key, new ValueRef(key, value));
        return ref != null ? ref.get() : null;
    }

    @Override
    public V remove(Object key) {
        ValueRef ref = backMap.remove(key);
        return ref != null ? ref.get() : null;
    }

    @Override
    public void clear() {
        backMap.clear();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        // we don't support null keys or values
        if (key == null || value == null) throw new NullPointerException("null keys or values not supported");
        ValueRef newRef = new ValueRef(key, value);
        while (true) {
            ValueRef prevRef = backMap.putIfAbsent(key, newRef);
            if (prevRef == null) return null;
            V prevValue = prevRef.get();
            if (prevValue != null) return prevValue;
            if (backMap.replace(key, prevRef, newRef)) return null;
            // concurrent modification -> retry
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        // we don't support null keys or values
        if (key == null || value == null) return false;
        while (true) {
            ValueRef currRef = backMap.get(key);
            if (currRef == null) return false;
            V currValue = currRef.get();
            if (currValue == null || !currValue.equals(value)) return false;
            if (backMap.remove(key, currRef)) return true;
            // concurrent modification -> retry
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // we don't support null keys or values
        if (key == null || oldValue == null) return false;
        if (newValue == null) throw new NullPointerException("null values not supported");
        ValueRef newRef = new ValueRef(key, newValue);
        while (true) {
            ValueRef prevRef = backMap.putIfAbsent(key, newRef);
            if (prevRef == null) return false;
            V prevValue = prevRef.get();
            if (prevValue == null || !prevValue.equals(newValue)) return false;
            if (backMap.replace(key, prevRef, newRef)) return true;
            // concurrent modification -> retry
        }
    }

    @Override
    public V replace(K key, V value) {
        // we don't support null keys or values
        if (key == null) return null;
        if (value == null) throw new NullPointerException("null values not supported");
        ValueRef newRef = new ValueRef(key, value);
        while (true) {
            ValueRef prevRef = backMap.get(key);
            if (prevRef == null) return null;
            V prevValue = prevRef.get();
            if (prevValue == null || !prevValue.equals(value)) return null;
            if (backMap.replace(key, prevRef, newRef)) return prevValue;
            // concurrent modification -> retry
        }
    }
}
