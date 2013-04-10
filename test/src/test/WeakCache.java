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
     * Look-up the value through the cache. This always evaluates the {@code subKeyFactory} function
     * and optionally evaluates {@code valueFactory} function if there is no entry in the cache for given pair of
     * (key, subKey) or the entry has already been cleared.
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

        Object cacheKey = CacheKey.valueOf(
            key,
            refQueue,
            Objects.requireNonNull(subKeyFactory.apply(key, parameter), "subKey returned by subKeyFactory is null")
        );
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
            // or a supplier that returned null (could be a cleared CacheValue
            // or a Factory that wasn't successful in installing the CacheValue)

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
                    // successfully replaced
                    // cleared CacheEntry / unsuccessful Factory
                    // with our Factory
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
            if (supplier != this) {
                // something changed while we were waiting:
                // might be that we were replaced by a CacheValue
                // or were removed because of failure ->
                // return null to signal WeakCache.get() to retry the loop
                return null;
            }
            // else still us (supplier == this)

            // create new value
            V value;
            try {
                value = Objects.requireNonNull(valueFactory.apply(key, parameter), "valueFactory returned null");
            }
            catch (RuntimeException | Error e) {
                // remove us on failure
                map.remove(cacheKey, this);
                // re-throw
                throw e;
            }
            catch (Throwable t) { // should not happen, but be conservative
                // remove us on failure
                map.remove(cacheKey, this);
                // re-throw wrapped
                throw new UndeclaredThrowableException(t);
            }

            // wrap value with CacheValue (WeakReference)
            CacheValue<V> cacheValue = new CacheValue<>(cacheKey, value, refQueue);

            // try replacing us with CacheValue (this should always succeed)
            if (!map.replace(cacheKey, this, cacheValue)) {
                throw new AssertionError("Somebody replaced us in the middle of constructing new value - should not happen");
            }

            // successfully replaced us with new CacheValue -> return the value wrapped by it
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
            // only remove if still mapped to same CacheValue
            // (by reference - CacheValue does not override Object.equals)
            return map.remove(cacheKey, this);
        }
    }

    private static final class CacheKey<K> extends WeakReference<K> implements Expungable {

        static <K> Object valueOf(K key, ReferenceQueue<Object> refQueue, Object subKey) {
            return key == null
                   // null key means we can't weakly reference it, so the subKey itself is appropriate
                   ? subKey
                   // non-null key requires wrapping with a WeakReference
                   : new CacheKey<>(key, refQueue, subKey);
        }

        private final int hash;
        private final Object subKey;

        private CacheKey(K key, ReferenceQueue<Object> refQueue, Object subKey) {
            super(key, refQueue);
            this.hash = key.hashCode() * 31 + subKey.hashCode();
            this.subKey = subKey;
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
