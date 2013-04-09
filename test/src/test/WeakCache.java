/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Cache mapping pairs of {@code (key, sub-key) -> value}. Keys and values are weakly but sub-keys are strongly referenced.
 * Keys are passed directly to {@link #get} method which also takes a {@code parameter}. Sub-keys are calculated from
 * keys and parameters using the {@code subKeyFactory} function passed to the {@link #WeakCache} constructor. Values are
 * calculated from keys and parameters using the {@code valueFactory} function passed to the {@link #WeakCache} constructor.
 * Keys can be null but sub-keys returned by {@code subKeyFactory} or values returned by {@code valueFactory} can not be null.
 * Entries are expunged from cache lazily on each invocation to {@link #get} method.
 *
 * @param <K> type of keys
 * @param <P> type of parameters
 * @param <V> type of values
 */
public final class WeakCache<K, P, V> {

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
    private final ConcurrentMap<Object, Supplier<V>> map = new ConcurrentHashMap<>();
    private final BiFunction<? super K, ? super P, ?> subKeyFactory;
    private final BiFunction<? super K, ? super P, ? extends V> valueFactory;

    /**
     * Construct an instance of {@code WeakCache}
     *
     * @param subKeyFactory a function mapping a pair of {@code (key, parameter) -> sub-key}
     * @param valueFactory  a function mapping a pair of {@code (key, parameter) -> value}
     * @throws NullPointerException if {@code subKeyFactory} or {@code valueFactory} is null.
     */
    public WeakCache(BiFunction<? super K, ? super P, ?> subKeyFactory,
                     BiFunction<? super K, ? super P, ? extends V> valueFactory) {
        this.subKeyFactory = Objects.requireNonNull(subKeyFactory, "subKeyFactory");
        this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
    }

    /**
     * Look-up the value through the cache.
     *
     * @param key       possibly null key
     * @param parameter parameter used together with key to create sub-key and value (should not be null)
     * @return the cached value (never null)
     * @throws NullPointerException if {@code parameter} passed in or {@code sub-key} calculated by
     *                              {@code subKeyFactory} or {@code value} calculated by {@code valueFactory} is null.
     * @throws RuntimeException     or subtype if {@code subKeyFactory} or {@code valueFactory} throws it
     * @throws Error                or subtype if {@code subKeyFactory} or {@code valueFactory} throws it
     */
    public V get(K key, P parameter) {
        Objects.requireNonNull(parameter, "parameter");

        expungeStaleEntries();

        Object cacheKey = CacheKey.valueOf(key, refQueue, subKeyFactory.apply(key, parameter));
        Supplier<V> supplier = map.get(cacheKey);
        Factory factory = null;

        while (true) {
            if (supplier != null) {
                // supplier might be a Factory or a CacheValue<V> instance
                V value = supplier.get();
                if (value != null) {
                    return value;
                }
            }
            // else no supplier in cache
            // or a supplier that returned null (can be cleared CacheValue or a Factory
            //   that wasn't successful in installing the CacheValue)

            // lazily construct a Factory
            if (factory == null) {
                factory = new Factory(key, parameter, cacheKey);
            }

            if (supplier == null) {
                supplier = map.putIfAbsent(cacheKey, factory);
                if (supplier == null) {
                    // successfully installed Factory
                    supplier = factory;
                }
                // else retry with winning supplier
            } else {
                if (map.replace(cacheKey, supplier, factory)) {
                    // successfully replaced cleared CacheEntry with Factory
                    supplier = factory;
                } else {
                    // retry with current supplier
                    supplier = map.get(cacheKey);
                }
            }
        }
    }

    private void expungeStaleEntries() {
        Expungable expungable;
        while ((expungable = (Expungable) refQueue.poll()) != null) {
            expungable.expungeFrom(map);
        }
    }

    private final class Factory implements Supplier<V> {

        private final K key;
        private final P parameter;
        private final Object cacheKey;

        Factory(K key, P parameter, Object cacheKey) {
            this.key = key;
            this.parameter = parameter;
            this.cacheKey = cacheKey;
        }

        @Override
        public synchronized V get() { // serialize access
            // re-check
            Supplier<V> supplier = map.get(cacheKey);
            V value;
            if (supplier != null && supplier != this) { // already replaced with CacheValue?
                value = supplier.get();
                if (value != null) { // and the CacheValue is not cleared yet
                    return value;
                }
            }
            // else still us (supplier == this)
            // or removed because of failure (supplier == null)
            // or replaced with a CacheValue that has already been cleared (supplier.get() == null)

            // create new value
            try {
                value = valueFactory.apply(key, parameter);
            }
            catch (RuntimeException | Error e) {
                // remove us on failure (if still in the map)
                if (supplier == this) {
                    map.remove(cacheKey, this);
                }
                // re-throw
                throw e;
            }
            catch (Throwable t) { // should not happen, but be conservative
                // remove us on failure (if still in the map)
                if (supplier == this) {
                    map.remove(cacheKey, this);
                }
                // re-throw wrapped
                throw new UndeclaredThrowableException(t);
            }

            // wrap it with CacheValue
            CacheValue<V> cacheValue = new CacheValue<>(cacheKey, value, refQueue);

            // try installing / replacing supplier with CacheValue
            if (supplier == this) {
                // still us -> replace with CacheValue (this should always succeed)
                if (!map.replace(cacheKey, this, cacheValue)) {
                    throw new AssertionError("Somebody replaced us in the middle of constructing new value - should not happen");
                }
            } else if (supplier == null) {
                // removed because of failure -> previous invocation to this Factory.get failed and so
                // we were removed from map but this invocation succeeded, so try to install the CacheValue anyway
                // (this can fail if the removed slot has already been taken by another Factory or CacheValue)
                supplier = map.putIfAbsent(cacheKey, cacheValue);
                if (supplier != null) { // already taken?
                    // rather than returning the value produced by us, return null to trigger retry in WeakCache.get()
                    return null;
                }
            } else {
                // replaced with CacheValue that has already been cleared -> try to replace it with the CacheValue
                // produced by us (this can fail if it was already replaced by another Factory or CacheValue)
                if (!map.replace(cacheKey, supplier, cacheValue)) { // already replaced with another supplier?
                    // rather than returning the value produced by us, return null to trigger retry in WeakCache.get()
                    return null;
                }
            }

            // successfully installed / replaced new CacheValue -> return the value wrapped by it
            return value;
        }
    }

    private interface Expungable {
        boolean expungeFrom(ConcurrentMap<?, ?> map);
    }

    private static final class CacheValue<V> extends WeakReference<V> implements Supplier<V>, Expungable {

        private final Object cacheKey;

        CacheValue(Object cacheKey, V value, ReferenceQueue<Object> refQueue) {
            super(value, refQueue);
            this.cacheKey = cacheKey;
        }

        @Override
        public boolean expungeFrom(ConcurrentMap<?, ?> map) {
            // only remove if still mapped to same CacheValue (by reference - using default Object.equals)
            return map.remove(cacheKey, this);
        }
    }

    private static final class CacheKey<K> extends WeakReference<K> implements Expungable {

        static <K> Object valueOf(K key, ReferenceQueue<Object> refQueue, Object subKey) {
            return key == null
                   // null key means we don't have to weakly reference it, so the subKey itself is appropriate
                   ? subKey
                   // non-null key requires wrapping with a WeakReference
                   : new CacheKey<>(key, refQueue, subKey);
        }

        private final int hash;
        private final Object subKey;

        private CacheKey(K key, ReferenceQueue<Object> refQueue, Object subKey) {
            super(key, refQueue);
            this.hash = key.hashCode() * 31 + subKey.hashCode();
            this.subKey = Objects.requireNonNull(subKey, "subKey");
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            CacheKey other;
            K thisKey;
            return obj == this ||
                   obj != null &&
                   obj.getClass() == this.getClass() &&
                   (thisKey = this.get()) != null &&
                   thisKey.equals((other = (CacheKey) obj).get()) &&
                   this.subKey.equals(other.subKey);
        }

        @Override
        public boolean expungeFrom(ConcurrentMap<?, ?> map) {
            // removing just by key is always safe here because when a CacheKey is cleared
            // it is only equal to itself (see equals method)...
            return map.remove(this) != null;
        }
    }
}
