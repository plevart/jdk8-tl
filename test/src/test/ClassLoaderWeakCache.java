/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class ClassLoaderWeakCache<K extends ClassLoaderWeakCache.Key, V> {

    private final ReferenceQueue<ClassLoader> clRefQueue = new ReferenceQueue<>();
    private final ReferenceQueue<V> vRefQueue = new ReferenceQueue<>();
    private final ConcurrentMap<K, Supplier<V>> map = new ConcurrentHashMap<>();
    private final Function<K, V> cacheLoader;

    public ClassLoaderWeakCache(Function<K, V> cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    public final V get(K key) {
        expungeStaleEntries();
        Supplier<V> supplier = map.get(key);
        while (true) {
            if (supplier != null) {
                V v = supplier.get();
                if (v != null) {
                    return v;
                }
            }

            Factory factory = new Factory(key);

            if (supplier == null) {
                supplier = map.putIfAbsent(key, factory);
                if (supplier == null) {
                    // successfully installed factory
                    return factory.get();
                }
                // else retry with winning supplier
            }
            else {
                if (map.replace(key, supplier, factory)) {
                    return factory.get();
                }
                else {
                    // retry with current supplier
                    supplier = map.get(key);
                }
            }
        }

    }

    private void expungeStaleEntries() {
        // 1st expunge cleared WeakReferences to values
        Reference<? extends V> vRef;
        while ((vRef = vRefQueue.poll()) != null) {
            @SuppressWarnings("unchecked")
            Value value = (Value) vRef;
            map.remove(value.key, value);
        }
        // 2nd expunge cleared WeakReferences to ClassLoaders
        Reference<? extends ClassLoader> clRef;
        while ((clRef = clRefQueue.poll()) != null) {
            @SuppressWarnings("unchecked")
            K key = (K) clRef;
            // it's always safe to remove such keys since when a WeakReference<ClassLoader>
            // is cleared, no other key instance can be equal to this key.
            map.remove(key);
        }
    }

    private final class Factory implements Supplier<V> {

        private final K key;

        Factory(K key) {
            this.key = key;
        }

        @Override
        public synchronized V get() {
            // re-check
            Supplier<V> supplier = map.get(key);
            if (supplier != null && supplier != this) {
                // already replaced with Value
                V val = supplier.get();
                if (val != null) {
                    // and the Value is not cleared yet
                    return val;
                }
            }
            // else still us or removed because of failure or the Value has
            // already been cleared
            return null;
        }
    }

    private final class Value extends WeakReference<V> implements Supplier<V> {

        final K key;

        Value(K key, V val) {
            super(val, vRefQueue);
            this.key = key;
        }
    }

    public abstract class Key<K extends Key> extends WeakReference<ClassLoader> {

        private final int clHash;

        public Key(ClassLoader classLoader) {
            super(classLoader, clRefQueue);
            this.clHash = classLoader.hashCode();
        }

        protected abstract int hashCodeImpl();

        protected abstract boolean equalsImpl(K otherKey);

        @Override
        public final int hashCode() {
            return getClass().hashCode() ^
                   clHash ^
                   hashCodeImpl();
        }

        @Override
        public final boolean equals(Object obj) {
            ClassLoader cl;
            K otherKey;
            return obj == this ||
                   obj != null &&
                   obj.getClass() == this.getClass() &&
                   (cl = this.get()) != null &&
                   (otherKey = (K) obj).get() == cl &&
                   equalsImpl(otherKey);
        }
    }
}
