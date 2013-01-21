package java.lang.reflect;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A base for {@link ConcurrentMap} implementations with the following characteristics:
 * <ul>
 * <li>weakly/softly referenced keys (like {@link WeakHashMap})</li>
 * <li>does not support null keys or values (like {@link ConcurrentHashMap})</li>
 * <li>the choice of using identity hashCode / comparison or hashCode()/equals() methods for keys</li>
 * <li> iterators are not fail-fast (like {@link ConcurrentHashMap})</li>
 * </ul>
 * This is a {@link ConcurrentMap} implementation backed by the {@link ConcurrentHashMap} so it
 * inherits the same scalability/concurrency characteristics. It uses a single background thread shared among all instances to
 * expunge stale weak/soft references, so no outside activity is needed for stale entries to be collected.
 */
public abstract class ReferenceConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {

    /**
     * A {@link ReferenceQueue} where weak/soft references to keys are enqueue-ed when cleared waiting to be
     * expunged by a background thread.
     */
    protected static final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    /**
     * A background thread doing the expunging work
     */
    private static final class ExpungeThread extends Thread {
        ExpungeThread(ThreadGroup group, String name) {
            super(group, name);
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    Reference<?> ref;
                    while ((ref = refQueue.remove()) != null && ref instanceof Key<?>)
                        ((Key<?>) ref).remove();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (
            ThreadGroup tgn = tg;
            tgn != null;
            tg = tgn, tgn = tg.getParent()
            )
            ;
        Thread expungeThread = new ExpungeThread(tg, "WeakConcurrentHashMap Expunge Thread");
        expungeThread.setDaemon(true);
        expungeThread.start();
    }

    /**
     * The backing map
     */
    protected final ConcurrentMap<Key<K>, V> map;

    public ReferenceConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    public ReferenceConcurrentHashMap(int initialCapacity, float loadFactor) {
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    public ReferenceConcurrentHashMap(int initialCapacity) {
        map = new ConcurrentHashMap<>(initialCapacity);
    }

    public ReferenceConcurrentHashMap() {
        map = new ConcurrentHashMap<>();
    }

    /**
     * These are copied from {@link ConcurrentHashMap} where they are package-private
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    public ReferenceConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this(
            Math.max(
                (int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY
            ),
            DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL
        );
        putAll(m);
    }

    // protected abstract methods/interfaces that are implemented in subclasses

    /**
     * An interface implemented by wrapper keys.
     *
     * @param <K> The type of wrapped key
     */
    protected interface Key<K> {
        /**
         * @return A wrapped key.
         */
        K get();

        /**
         * Optional operation. If class implementing this interface extend {@link Reference} and is constructed using
         * {@link #refQueue} as a reference queue, then this method will be called by a background thread after this
         * reference is enqueue-ed. The purpose of this method is to remove the corresponding entry from the {@link #map}.
         */
        void remove();
    }

    /**
     * @param key  A non-null key to wrap
     * @param <KK> the type of key
     * @return A wrapper key suitable for performing look-ups into the backing {@link #map}.
     *         Wrapper keys returned from this method will never end up being held in the backing map.
     * @see #wrapForPut
     */
    protected abstract <KK> Key<KK> wrapForLookup(KK key);

    /**
     * @param key A non-null key to wrap
     * @return A wrapper key suitable for performing put operations.
     *         Wrapper keys returned from this method will eventually be retained by the backing {@link #map} so
     *         they are expected to be a subclass of {@link Reference} and be constructed with a {@link #refQueue}
     *         reference queue.
     * @see #wrapForLookup
     */
    protected abstract Key<K> wrapForPut(K key);

    // public API

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(wrapForPut(key), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(wrapForLookup(key), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return map.replace(wrapForLookup(key), oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return map.replace(wrapForLookup(key), value);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(wrapForLookup(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(wrapForLookup(key));
    }

    @Override
    public V put(K key, V value) {
        return map.put(wrapForPut(key), value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(wrapForLookup(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            map.put(wrapForPut(e.getKey()), e.getValue());
    }

    @Override
    public void clear() {
        clear();
    }

    private Set<K> keySet;

    @Override
    public Set<K> keySet() {
        if (keySet == null)
            keySet = new AbstractSet<K>() {
                @Override
                public Iterator<K> iterator() {
                    final Iterator<Key<K>> iter = map.keySet().iterator();
                    return new Iterator<K>() {
                        private K next;

                        @Override
                        public boolean hasNext() {
                            while (next == null && iter.hasNext())
                                next = iter.next().get();
                            return next != null;
                        }

                        @Override
                        public K next() {
                            if (hasNext()) {
                                K k = next;
                                next = null;
                                return k;
                            } else {
                                throw new NoSuchElementException();
                            }
                        }

                        @Override
                        public void remove() {
                            iter.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return ReferenceConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    ReferenceConcurrentHashMap.this.clear();
                }

                @Override
                public boolean contains(Object o) {
                    return ReferenceConcurrentHashMap.this.containsKey(o);
                }

                @Override
                public boolean remove(Object o) {
                    return ReferenceConcurrentHashMap.this.remove(o) != null;
                }
            };

        return keySet;
    }

    private Collection<V> values;

    @Override
    public Collection<V> values() {
        if (values == null)
            values = new AbstractCollection<V>() {
                @Override
                public Iterator<V> iterator() {
                    final Iterator<Entry<Key<K>, V>> iter = map.entrySet().iterator();
                    return new Iterator<V>() {
                        V next;

                        @Override
                        public boolean hasNext() {
                            while (next == null && iter.hasNext()) {
                                Entry<Key<K>, V> e = iter.next();
                                if (e.getKey().get() != null)
                                    next = e.getValue();
                            }
                            return next != null;
                        }

                        @Override
                        public V next() {
                            if (hasNext()) {
                                V v = next;
                                next = null;
                                return v;
                            } else {
                                throw new NoSuchElementException();
                            }
                        }

                        @Override
                        public void remove() {
                            iter.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return ReferenceConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    ReferenceConcurrentHashMap.this.clear();
                }

                @Override
                public boolean contains(Object o) {
                    return ReferenceConcurrentHashMap.this.containsValue(o);
                }

                @Override
                public boolean remove(Object o) {
                    if (o == null) return false; // we don't support null values
                    return super.remove(o);
                }
            };

        return values;
    }

    private Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null)
            entrySet = new AbstractSet<Entry<K, V>>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    final Iterator<Entry<Key<K>, V>> iter = map.entrySet().iterator();
                    return new Iterator<Entry<K, V>>() {
                        private Entry<K, V> next;

                        @Override
                        public boolean hasNext() {
                            while (next == null && iter.hasNext()) {
                                Entry<Key<K>, V> e = iter.next();
                                K key = e.getKey().get();
                                next = key == null ? null : new AbstractMap.SimpleEntry<K, V>(key, e.getValue());
                            }
                            return next != null;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (hasNext()) {
                                Entry<K, V> e = next;
                                next = null;
                                return e;
                            } else {
                                throw new NoSuchElementException();
                            }
                        }

                        @Override
                        public void remove() {
                            iter.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return ReferenceConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    ReferenceConcurrentHashMap.this.clear();
                }

                @Override
                public boolean contains(Object o) {
                    Map.Entry<?, ?> entry;
                    V value;
                    return o instanceof Map.Entry &&
                           (value = get((entry = (Map.Entry<?, ?>) o).getKey())) != null &&
                           value.equals(entry.getValue());
                }

                @Override
                public boolean remove(Object o) {
                    Map.Entry<K, V> entry;
                    return o instanceof Map.Entry &&
                           ReferenceConcurrentHashMap.this.remove(
                               (entry = (Map.Entry<K, V>) o).getKey(),
                               entry.getValue()
                           );
                }
            };

        return entrySet;
    }
}
