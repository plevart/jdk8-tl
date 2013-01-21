package java.lang.reflect;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A concrete {@link ReferenceConcurrentHashMap} implementation with weak keys
 * using identity hashCode / identity comparison for keys.
 */
public class IdentityWeakConcurrentHashMap<K, V> extends ReferenceConcurrentHashMap<K, V> {

    public IdentityWeakConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
    }

    public IdentityWeakConcurrentHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public IdentityWeakConcurrentHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public IdentityWeakConcurrentHashMap() {
        super();
    }

    public IdentityWeakConcurrentHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    private static final class StrongKey<K> implements Key<K> {
        private final K key;

        StrongKey(K key) {
            if (key == null) throw new NullPointerException();
            this.key = key;
        }

        @Override
        public K get() {
            return key;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Key && key == ((Key) obj).get();
        }

        @Override
        public void remove() {
            throw new AssertionError("Should not be called");
        }
    }

    private final class WeakKey extends WeakReference<K> implements Key<K> {
        private final int hashCode;

        WeakKey(K key) {
            super(key, refQueue);
            if (key == null) throw new NullPointerException();
            hashCode = System.identityHashCode(key);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            K key = get();
            if (key == null) {
                // already cleared -> only equal to itself
                return this == obj;
            } else {
                return this == obj || obj instanceof Key && key == ((Key) obj).get();
            }
        }

        @Override
        public void remove() {
            System.out.println(
                "Removing WeakKey(" + Integer.toHexString(hashCode) +
                ") from IdentityWeakConcurrentHashMap(" + Integer.toHexString(System.identityHashCode(IdentityWeakConcurrentHashMap.this))
            );
            map.remove(this);
        }
    }

    @Override
    protected <KK> Key<KK> wrapForLookup(KK key) {
        return new StrongKey<>(key);
    }

    @Override
    protected Key<K> wrapForPut(K key) {
        return new WeakKey(key);
    }
}
