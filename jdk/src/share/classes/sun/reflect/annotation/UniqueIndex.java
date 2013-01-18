package sun.reflect.annotation;

import java.lang.reflect.Array;
import java.util.*;

/**
 * An abstract immutable {@link Map} implementation composed of non-null values given at
 * construction time via {@link #UniqueIndex(java.util.Collection)} constructor
 * and non-null unique keys extracted from values by {@link #extractKey} method implemented
 * in concrete subclasses.<p>
 * This implementation wraps a single internal array of values sorted by {@link #hashCode}s
 * of extracted keys and employs a binary search by given key's hashCode, optionally followed
 * by linear search of equal hasCode neighbourhood, to find an entry.
 * {@link #get} and {@link #containsKey} method's time complexity is log2(n).
 */
abstract class UniqueIndex<K, V> implements Map<K, V> {

    private final V[] values;

    /**
     * Constructs an immutable map instance from given values.
     *
     * @param valuesCollection a collection of non-null values
     * @throws NonUniqueKeyException if not all keys extracted from values were unique.
     * @throws NullPointerException  if given collection was null or any element was null or any extracted key was null.
     */
    public UniqueIndex(Collection<? extends V> valuesCollection) throws NonUniqueKeyException, NullPointerException {
        this(valuesCollection.toArray((V[]) new Object[valuesCollection.size()]));
    }

    private UniqueIndex(V[] values) throws NonUniqueKeyException, NullPointerException {
        Arrays.sort(
            values,
            new Comparator<V>() {
                @Override
                public int compare(V v1, V v2) {
                    int h1 = extractKey(v1).hashCode();
                    int h2 = extractKey(v2).hashCode();
                    return h1 < h2 ? -1 : (h1 > h2 ? 1 : 0);
                }
            }
        );

        K prevKey = null;
        for (V value : values) {
            K key = extractKey(value);
            if (prevKey != null && key.equals(prevKey))
                throw new NonUniqueKeyException("Non unique key: " + key);
            prevKey = key;
        }

        this.values = values;
    }

    /**
     * Implemented by subclasses to extract a unique non-null key from the given non-null value
     *
     * @param value a non-null value
     * @return a non-null unique key extracted from the value
     */
    protected abstract K extractKey(V value);

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        // we don't support null values
        if (value == null) return false;
        // linear search
        for (V v : values)
            if (v.equals(value))
                return true;
        return false;
    }

    @Override
    public V get(Object key) {
        // we don't support null keys
        if (key == null) return null;
        int keyHash = key.hashCode();
        // binary search by keyHash
        int low = 0;
        int high = values.length - 1;
        K k = null;
        V v = null;
        int i = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            k = extractKey(v = values[mid]);
            int kh = k.hashCode();
            if (kh < keyHash)
                low = mid + 1;
            else if (kh > keyHash)
                high = mid - 1;
            else {
                // keyHash found
                i = mid;
                break;
            }
        }
        // not found
        if (i < 0) return null;
        // 1st check a direct hit
        if (k.equals(key)) return v;
        // 2nd check the neighborhood whit same key hashes
        // forwards...
        for (int j = i + 1; j < values.length; j++) {
            k = extractKey(v = values[j]);
            if (k.hashCode() != keyHash)
                break;
            if (k.equals(key))
                return v;
        }
        // ...and backwards...
        for (int j = i - 1; j >= 0; j--) {
            k = extractKey(v = values[j]);
            if (k.hashCode() != keyHash)
                break;
            if (k.equals(key))
                return v;
        }
        // none found
        return null;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < values.length;
                    }

                    @Override
                    public K next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return extractKey(values[i++]);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return UniqueIndex.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < values.length;
                    }

                    @Override
                    public V next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return values[i++];
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return UniqueIndex.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsValue(o);
            }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < values.length;
                    }

                    @Override
                    public Entry<K, V> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        V v = values[i++];
                        return new AbstractMap.SimpleEntry<>(extractKey(v), v);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return UniqueIndex.this.size();
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof Entry) {
                    Entry<?, ?> e = (Entry<?, ?>) o;
                    V v = get(e.getKey());
                    return v != null && v.equals(e.getValue());
                }
                return false;
            }
        };
    }

    /**
     * Similar to calling {@link #values()}.{@link Collection#toArray(Object[]) toArray(T[])} but more optimal.
     *
     * @param arrayComponentType the component type of array constructed and returned.
     * @param <T> the type parameter for the component type of array returned.
     * @return new array with requested component type filled with values of this map.
     * @throws ArrayStoreException if any value of this map can not be assigned to given array's component type
     */
    public <T> T[] toValuesArray(Class<T> arrayComponentType) {
        @SuppressWarnings("unchecked")
        T[] valuesArray = (T[]) Array.newInstance(arrayComponentType, values.length);
        System.arraycopy(values, 0, valuesArray, 0, values.length);
        return valuesArray;
    }

    /**
     * Thrown when trying to construct a unique index with a collection holding elements with non unique keys
     */
    public static class NonUniqueKeyException extends IllegalArgumentException {
        public NonUniqueKeyException(String s) {
            super(s);
        }
    }
}
