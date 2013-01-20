package java.lang.reflect;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Like {@link WeakHashMap} and {@link ConcurrentHashMap} at once.
 */
class WeakConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {

    private interface Key<K> {
        K get();
    }

    private interface Removable {
        void remove();
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
            return obj instanceof Key && ((Key) obj).get() == key;
        }
    }

    private static final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    private final class WeakKey extends WeakReference<K> implements Key<K>, Removable {
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
            }
            else {
                return obj instanceof Key && ((Key) obj).get() == key;
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

    private static final class EntryPurger extends Thread {
        EntryPurger(ThreadGroup group, String name) {
            super(group, name);
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    Reference<?> ref;
                    while ((ref = refQueue.remove()) != null && ref instanceof Removable)
                        ((Removable) ref).remove();
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
        Thread purger = new EntryPurger(tg, "WeakConcurrentHashMap Entry Purger");
        purger.setDaemon(true);
        purger.start();
    }

    // the backing map
    private final ConcurrentMap<Key<K>, V> map = new ConcurrentHashMap<>();

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(new WeakKey(key), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(new StrongKey<>(key), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return map.replace(new StrongKey<>(key), oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return map.replace(new StrongKey<>(key), value);
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
        return map.containsKey(new StrongKey<>(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(new StrongKey<>(key));
    }

    @Override
    public V put(K key, V value) {
        return map.put(new WeakKey(key), value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(new StrongKey<>(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            map.put(new WeakKey(e.getKey()), e.getValue());
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
                            }
                            else {
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
                    return WeakConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    WeakConcurrentHashMap.this.clear();
                }

                @Override
                public boolean contains(Object o) {
                    return WeakConcurrentHashMap.this.containsKey(o);
                }

                @Override
                public boolean remove(Object o) {
                    return WeakConcurrentHashMap.this.remove(o) != null;
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
                            }
                            else {
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
                    return WeakConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    WeakConcurrentHashMap.this.clear();
                }

                @Override
                public boolean contains(Object o) {
                    return WeakConcurrentHashMap.this.containsValue(o);
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
                            }
                            else {
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
                    return WeakConcurrentHashMap.this.size();
                }

                @Override
                public void clear() {
                    WeakConcurrentHashMap.this.clear();
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
                        WeakConcurrentHashMap.this.remove(
                            (entry = (Map.Entry<K, V>) o).getKey(),
                            entry.getValue()
                        );
                }
            };

        return entrySet;
    }
}
