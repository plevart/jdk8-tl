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
 * This {@link WeakCache} implementation uses single<p>
 * {@code ConcurrentMap<CacheKey<K>, ConcurrentMap<SubKey, CacheValue<V>>>}
 * as the main backing data-structure.
 */
final class TwoLevelWeakCache<K, P, V> implements WeakCache<K, P, V> {

    private final ReferenceQueue<Object> refQueue
        = new ReferenceQueue<>();
    private final ConcurrentMap<Object, ConcurrentMap<Object, Supplier<V>>> map
        = new ConcurrentHashMap<>();
    private final ConcurrentMap<Supplier<V>, Boolean> reverseMap;
    private final BiFunction<? super K, ? super P, ?> subKeyFactory;
    private final BiFunction<? super K, ? super P, ? extends V> valueFactory;

    /**
     * Construct an instance of {@code TwoLevelWeakCache}
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
    public TwoLevelWeakCache(
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
            refQueue
        );

        // lazily install the 2nd level valuesMap for the particular cacheKey
        ConcurrentMap<Object, Supplier<V>> valuesMap = map.get(cacheKey);
        if (valuesMap == null) {
            ConcurrentMap<Object, Supplier<V>> oldValuesMap = map.putIfAbsent(
                cacheKey,
                valuesMap = new ConcurrentHashMap<>()
            );
            if (oldValuesMap != null) {
                valuesMap = oldValuesMap;
            }
        }

        // create subKey and retrieve the possible Supplier<V> stored by that
        // subKey from valuesMap
        Object subKey = Objects.requireNonNull(
            subKeyFactory.apply(key, parameter),
            "subKeyFactory returned null"
        );
        Supplier<V> supplier = valuesMap.get(subKey);
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
                factory = new Factory(key, parameter, subKey, valuesMap);
            }

            if (supplier == null) {
                supplier = valuesMap.putIfAbsent(subKey, factory);
                if (supplier == null) {
                    // successfully installed Factory
                    supplier = factory;
                }
                // else retry with winning supplier
            } else {
                if (valuesMap.replace(subKey, supplier, factory)) {
                    // successfully replaced
                    // cleared CacheEntry / unsuccessful Factory
                    // with our Factory
                    supplier = factory;
                } else {
                    // retry with current supplier
                    supplier = valuesMap.get(subKey);
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
        private final Object subKey;
        private final ConcurrentMap<Object, Supplier<V>> valuesMap;

        Factory(K key, P parameter, Object subKey, ConcurrentMap<Object, Supplier<V>> valuesMap) {
            this.key = key;
            this.parameter = parameter;
            this.subKey = subKey;
            this.valuesMap = valuesMap;
        }

        @Override
        public synchronized V get() { // serialize access
            // re-check
            Supplier<V> supplier = valuesMap.get(subKey);
            if (supplier != this) {
                // something changed while we were waiting:
                // might be that we were replaced by a CacheValue
                // or were removed because of failure ->
                // return null to signal TwoLevelWeakCache.get() to retry
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
                valuesMap.remove(subKey, this);
                // re-throw
                throw e;
            } catch (Throwable t) { // should not happen, but be conservative
                // remove us on failure
                valuesMap.remove(subKey, this);
                // re-throw wrapped
                throw new UndeclaredThrowableException(t);
            }

            // wrap value with CacheValue (WeakReference)
            CacheValue<V> cacheValue = new CacheValue<>(
                value,
                refQueue,
                subKey,
                valuesMap
            );

            // try replacing us with CacheValue (this should always succeed)
            if (valuesMap.replace(subKey, this, cacheValue)) {
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
            ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> map,
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
     * {@link TwoLevelWeakCache#containsValue} method so that we are not
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
     * reference to {@code subKey} o that it can implement {@link Expungable}
     * by removing the corresponding entries from the maps.
     */
    private static final class CacheValue<V>
        extends WeakReference<V>
        implements Value<V>, Expungable {
        private final int hash;
        private final Object subKey;
        private final ConcurrentMap<Object, Supplier<V>> valuesMap;

        CacheValue(
            V value,
            ReferenceQueue<Object> refQueue,
            Object subKey,
            ConcurrentMap<Object, Supplier<V>> valuesMap
        ) {
            super(value, refQueue);
            this.hash = System.identityHashCode(value); // compare by identity
            this.subKey = subKey;
            this.valuesMap = valuesMap;
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
            ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> map,
            ConcurrentMap<?, Boolean> reverseMap
        ) {
            // only remove if still mapped to same Supplier
            valuesMap.remove(subKey, this);
            // remove from reverseMap too...
            if (reverseMap != null) {
                reverseMap.remove(this);
            }
        }
    }

    /**
     * CacheKey containing a weekly referenced {@code key}. It also implements
     * {@link Expungable} so it can clean-up when the weekly referenced key is
     * garbage collected. The containing {@code key} is compared by identity.
     */
    private static final class CacheKey<K>
        extends WeakReference<K>
        implements Expungable {

        // a replacement for null keys
        private static final Object NULL_KEY = new Object();

        static <K> Object valueOf(K key, ReferenceQueue<Object> refQueue) {
            return key == null
                   // null key means we can't weakly reference it,
                   // so we substitute it with a NULL_KEY
                   ? NULL_KEY
                   // non-null key requires wrapping with a WeakReference
                   : new CacheKey<>(key, refQueue);
        }

        private final int hash;

        private CacheKey(K key, ReferenceQueue<Object> refQueue) {
            super(key, refQueue);
            this.hash = System.identityHashCode(key);  // by identity
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
                   thisKey == (other = (CacheKey<?>) obj).get();
        }

        @Override
        public void expungeFrom(
            ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> map,
            ConcurrentMap<?, Boolean> reverseMap
        ) {
            // removing just by key is always safe here because after a CacheKey
            // is cleared and enqueue-ed it is only equal to itself
            // (see equals method)...
            ConcurrentMap<?, ?> valuesMap = map.remove(this);
            // remove also from reverseMap if needed
            if (valuesMap != null && reverseMap != null) {
                for (Object cacheValue : valuesMap.values()) {
                    reverseMap.remove(cacheValue);
                }
            }
        }
    }
}
