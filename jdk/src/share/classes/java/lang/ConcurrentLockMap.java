package java.lang;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
final class ConcurrentLockMap<K> extends ConcurrentHashMap<K, Object> {

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    private static final class WeakKey<K> extends WeakReference<Object> {
        final K key;

        WeakKey(K key, Object lock, ReferenceQueue<? super Object> q) {
            super(lock, q);
            this.key = key;
        }
    }

    public Object createOrGet(K key) {
        Object lock = new Object();
        WeakKey<K> ref = new WeakKey<>(key, lock, refQueue);
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        WeakKey<K>  oldRef = (WeakKey<K> ) super.putIfAbsent(key, ref);
        Object oldLock;
        if (oldRef != null) {
            if ((oldLock = oldRef.get()) != null) {
            lock = oldLock;
            }
            else {

            }
        }
    }

    // overriden Map methods

    @Override
    public Object get(Object key) {
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        WeakReference<Object> ref = (WeakReference<Object>) super.get(key);
        return ref == null ? null : ref.get();
    }

    private void expungeStaleEntries() {

    }

    // unsupported Map methods (not needed for maintaining locks or even dangerous)

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(K key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object putIfAbsent(K key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object replace(K key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<K> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<Object> elements() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException();
    }
}
