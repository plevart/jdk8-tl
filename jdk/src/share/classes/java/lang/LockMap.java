package java.lang;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * A map of weakly referenced lock Objects, keyed by common values such as String, etc.<p>
 * Although this is a subclass of {@link ConcurrentHashMap}, all it's methods except:
 * {@link #get(Object)} and {@link #getOrCreate(Object)} are unsupported and always
 * throw {@link UnsupportedOperationException}.
 */
public final class LockMap<K> extends ConcurrentHashMap<K, Object> {

    private static final boolean keepStats = true;
    private static final LongAdder createCount = new LongAdder();
    private static final LongAdder returnOldCount = new LongAdder();
    private static final LongAdder replaceCount = new LongAdder();
    private static final LongAdder getNullCount = new LongAdder();
    private static final LongAdder getNonNullCount = new LongAdder();
    private static final LongAdder expungeCount = new LongAdder();

    public static String getStats() {
        if (!keepStats) return null;
        StringBuilder sb = new StringBuilder()
            .append("      create: ").append(createCount.sum()).append("\n")
            .append("  return old: ").append(returnOldCount.sum()).append("\n")
            .append("     replace: ").append(replaceCount.sum()).append("\n")
            .append("    get null: ").append(getNullCount.sum()).append("\n")
            .append("get non-null: ").append(getNonNullCount.sum()).append("\n")
            .append("     expunge: ").append(expungeCount.sum()).append("\n");
        return sb.toString();
    }

    public static String getAndResetStats() {
        if (!keepStats) return null;
        StringBuilder sb = new StringBuilder()
            .append("      create: ").append(createCount.sumThenReset()).append("\n")
            .append("  return old: ").append(returnOldCount.sumThenReset()).append("\n")
            .append("     replace: ").append(replaceCount.sumThenReset()).append("\n")
            .append("    get null: ").append(getNullCount.sumThenReset()).append("\n")
            .append("get non-null: ").append(getNonNullCount.sumThenReset()).append("\n")
            .append("     expunge: ").append(expungeCount.sumThenReset()).append("\n");
        return sb.toString();
    }

    private final Object owner;

    LockMap(Object owner) {
        this.owner = owner;
    }

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    private static final class LockRef<K> extends WeakReference<Object> {
        final K key;

        LockRef(K key, Object lock, ReferenceQueue<? super Object> q) {
            super(lock, q);
            this.key = key;
        }
    }

    /**
     * Gets or creates the unique lock object associated with the {@code key}.
     *
     * @param key The key to look-up or to store in the map of locks.
     * @return The non-null unique lock object associated with the {@code key}.
     */
    public Object getOrCreate(K key) {
        // the most common situation is that the key is new, so optimize fast-path accordingly
        Object lock = new Object();
        LockRef<K> ref = new LockRef<>(key, lock, refQueue);
        expungeStaleEntries();
        for (; ; ) {
            @SuppressWarnings("unchecked")
            LockRef<K> oldRef = (LockRef<K>) super.putIfAbsent(key, ref);
            if (oldRef == null) {
                if (keepStats) createCount.increment();
                return lock;
            }
            else {
                Object oldLock = oldRef.get();
                if (oldLock != null) {
                    if (keepStats) returnOldCount.increment();
                    return oldLock;
                }
                else if (super.replace(key, oldRef, ref)) {
                    if (keepStats) replaceCount.increment();
                    return lock;
                }
            }
        }
    }

    /**
     * Gets  the unique lock object associated with the {@code key}.
     *
     * @param key The key to look-up in the map of locks.
     * @return The unique lock object associated with the {@code key} if one exists
     *         or null if not found.
     */
    @Override
    public Object get(Object key) {
        expungeStaleEntries();
        @SuppressWarnings("unchecked")
        WeakReference<Object> ref = (WeakReference<Object>) super.get(key);
        Object lock = ref == null ? null : ref.get();
        if (keepStats) {
            if (lock == null)
                getNullCount.increment();
            else
                getNonNullCount.increment();
        }
        return lock;
    }

    private void expungeStaleEntries() {
        LockRef<K> ref;
        while ((ref = (LockRef<K>) refQueue.poll()) != null) {
            super.remove(ref.key, ref);
            if (keepStats) expungeCount.increment();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
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
