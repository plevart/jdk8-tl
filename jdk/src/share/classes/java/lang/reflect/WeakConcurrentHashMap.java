package java.lang.reflect;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A concrete {@link ReferenceConcurrentHashMap} implementation with weak keys
 * using hashCode() / equals() comparison for keys.
 */
public class WeakConcurrentHashMap<K, V> extends ReferenceConcurrentHashMap<K, V> {

    public WeakConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
    }

    public WeakConcurrentHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public WeakConcurrentHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public WeakConcurrentHashMap() {
        super();
    }

    public WeakConcurrentHashMap(Map<? extends K, ? extends V> m) {
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
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Key && key.equals(((Key) obj).get());
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
            hashCode = key.hashCode();
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
                return this == obj || obj instanceof Key && key.equals(((Key) obj).get());
            }
        }

        @Override
        public void remove() {
            System.out.println(
                "Removing WeakKey(" + Integer.toHexString(hashCode) +
                ") from WeakConcurrentHashMap(" + Integer.toHexString(System.identityHashCode(WeakConcurrentHashMap.this))
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
