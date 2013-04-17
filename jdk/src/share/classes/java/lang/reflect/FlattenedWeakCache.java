/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package java.lang.reflect;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * This {@link WeakCache} implementation uses single
 * {@code ConcurrentMap<CacheKey<K>, CacheValue<V>>} as the main backing
 * data-structure where {@code key} and {@code subKey} are flattened into
 * a single {@link CacheKey}.
 */
final class FlattenedWeakCache<K, P, V> implements WeakCache<K, P, V> {

    private final ReferenceQueue<Object> refQueue
        = new ReferenceQueue<>();
    private final ConcurrentMap<Object, Supplier<V>> map
        = new ConcurrentHashMap<>();
    private final ConcurrentMap<Supplier<V>, Boolean> reverseMap;
    private final BiFunction<? super K, ? super P, ?> subKeyFactory;
    private final BiFunction<? super K, ? super P, ? extends V> valueFactory;

    /**
     * Construct an instance of {@code FlattenedWeakCache}
     *
     * @param subKeyFactory                 a function mapping a pair of
     *                                      {@code (key, parameter) -> sub-key}
     * @param valueFactory                  a function mapping a pair of
     *                                      {@code (key, parameter) -> value}
     * @param supportContainsValueOperation if true the cache also maintains an
     *                                      inverse index of cached values to
     *                                      support {@link #containsValue} and
     *                                      {@link #size} optional operations
     * @throws NullPointerException if {@code subKeyFactory} or
     *                              {@code valueFactory} is null.
     */
    public FlattenedWeakCache(
        BiFunction<? super K, ? super P, ?> subKeyFactory,
        BiFunction<? super K, ? super P, ? extends V> valueFactory,
        boolean supportContainsValueOperation
    ) {
        this.subKeyFactory = Objects.requireNonNull(
            subKeyFactory,
            "subKeyFactory"
        );
        this.valueFactory = Objects.requireNonNull(
            valueFactory,
            "valueFactory"
        );
        this.reverseMap = supportContainsValueOperation
                          ? new ConcurrentHashMap<>()
                          : null;
    }

    public final V get(K key, P parameter) {
        Objects.requireNonNull(parameter, "parameter");

        expungeStaleEntries();

        Object cacheKey = CacheKey.valueOf(
            key,
            refQueue,
            Objects.requireNonNull(
                subKeyFactory.apply(key, parameter),
                "subKeyFactory returned null"
            )
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

    @Override
    public boolean containsValue(V value) {
        if (reverseMap == null)
            throw new UnsupportedOperationException();
        expungeStaleEntries();
        return value != null && reverseMap.containsKey(new LookupValue<>(value));
    }

    @Override
    public int size() {
        if (reverseMap == null)
            throw new UnsupportedOperationException();
        expungeStaleEntries();
        return reverseMap.size();
    }

    private void expungeStaleEntries() {
        Expungable expungable;
        while ((expungable = (Expungable) refQueue.poll()) != null) {
            expungable.expungeFrom(map, reverseMap);
        }
    }

    /**
     * A factory {@link Supplier} that implements the lazy synchronized
     * construction of the value and installment of it into the cache.
     */
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
                // return null to signal FlattenedWeakCache.get() to retry
                // the loop
                return null;
            }
            // else still us (supplier == this)

            // create new value
            V value;
            try {
                value = Objects.requireNonNull(
                    valueFactory.apply(key, parameter),
                    "valueFactory returned null"
                );
            } catch (RuntimeException | Error e) {
                // remove us on failure
                map.remove(cacheKey, this);
                // re-throw
                throw e;
            } catch (Throwable t) { // should not happen, but be conservative
                // remove us on failure
                map.remove(cacheKey, this);
                // re-throw wrapped
                throw new UndeclaredThrowableException(t);
            }

            // wrap value with CacheValue (WeakReference)
            CacheValue<V> cacheValue = new CacheValue<>(
                value,
                refQueue,
                cacheKey
            );

            // try replacing us with CacheValue (this should always succeed)
            if (map.replace(cacheKey, this, cacheValue)) {
                // put also in reverseMap if needed
                if (reverseMap != null) {
                    reverseMap.put(cacheValue, Boolean.TRUE);
                }
            } else {
                throw new AssertionError(
                    "Replaced by ghost - should not happen"
                );
            }

            // successfully replaced us with new CacheValue -> return the value
            // wrapped by it
            return value;
        }
    }

    /**
     * An interface implemented by WeakReference subclasses with a single method
     * {@link #expungeFrom} which is used to clean-up entry from maps.
     */
    private interface Expungable {
        void expungeFrom(
            ConcurrentMap<?, ?> map,
            ConcurrentMap<?, Boolean> reverseMap
        );
    }

    /**
     * Common type of value suppliers that are holding a referent.
     * The {@link #equals} and {@link #hashCode} of implementations is defined
     * to compare the referent by identity.
     */
    private interface Value<V> extends Supplier<V> {}

    /**
     * An optimized {@link Value} used to look-up the value in
     * {@link FlattenedWeakCache#containsValue} method so that we are not
     * constructing the whole {@link CacheValue} just to look-up the referent.
     */
    private static final class LookupValue<V> implements Value<V> {

        private final V value;

        LookupValue(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(value); // compare by identity
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this ||
                   obj instanceof Value &&
                   this.value == ((Value<?>) obj).get();  // compare by identity
        }
    }

    /**
     * A {@link Value} that weakly references the referent and also holds a
     * reference to {@code cacheKey} so that it can implement {@link Expungable}
     * by removing the corresponding entries from the maps.
     */
    private static final class CacheValue<V>
        extends WeakReference<V>
        implements Value<V>, Expungable {
        private final int hash;
        private final Object cacheKey;

        CacheValue(V value,
                   ReferenceQueue<Object> refQueue,
                   Object cacheKey
        ) {
            super(value, refQueue);
            this.hash = System.identityHashCode(value); // compare by identity
            this.cacheKey = cacheKey;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            V value;
            return obj == this ||
                   obj instanceof Value &&
                   // cleared CacheValue is only equal to itself
                   (value = get()) != null &&
                   value == ((Value<?>) obj).get(); // compare by identity
        }

        @Override
        public void expungeFrom(
            ConcurrentMap<?, ?> map,
            ConcurrentMap<?, Boolean> reverseMap
        ) {
            // only remove if still mapped to same Supplier
            map.remove(cacheKey, this);
            // remove from reverseMap too...
            if (reverseMap != null) {
                reverseMap.remove(this);
            }
        }
    }

    /**
     * CacheKey containing a weekly referenced {@code key} and
     * strongly referenced {@code subKey}. It also implements {@link Expungable}
     * so it can clean-up when the weekly referenced key is garbage collected.
     * The  containing {@code key} is compared by identity and the
     * {@code subKey} by value.
     */
    private static final class CacheKey<K>
        extends WeakReference<K>
        implements Expungable {

        static <K> Object valueOf(
            K key,
            ReferenceQueue<Object> refQueue,
            Object subKey
        ) {
            return key == null
                   // null key means we can't weakly reference it,
                   // so the subKey itself is appropriate
                   ? subKey
                   // non-null key requires wrapping with a WeakReference
                   : new CacheKey<>(key, refQueue, subKey);
        }

        private final int hash;
        private final Object subKey;

        private CacheKey(K key, ReferenceQueue<Object> refQueue, Object subKey) {
            super(key, refQueue);
            this.hash = System.identityHashCode(key) * 31 + // by identity
                        subKey.hashCode();                  // by value
            this.subKey = subKey;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            CacheKey other;
            K thisKey;
            return obj == this ||
                   obj != null &&
                   obj.getClass() == this.getClass() &&
                   // cleared CacheKey is only equal to itself
                   (thisKey = this.get()) != null &&
                   // compare key by identity
                   thisKey == (other = (CacheKey<?>) obj).get() &&
                   // compare subKey by value
                   this.subKey.equals(other.subKey);
        }

        @Override
        public void expungeFrom(
            ConcurrentMap<?, ?> map,
            ConcurrentMap<?, Boolean> reverseMap
        ) {
            // removing just by key is always safe here because after a CacheKey
            // is cleared and enqueue-ed it is only equal to itself
            // (see equals method)...
            Object cacheValue = map.remove(this);
            // remove also from reverseMap if needed
            if (cacheValue != null && reverseMap != null) {
                reverseMap.remove(cacheValue);
            }
        }
    }
}
