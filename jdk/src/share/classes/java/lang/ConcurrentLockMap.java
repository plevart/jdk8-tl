package java.lang;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
final class ConcurrentLockMap<K> extends ConcurrentHashMap<K, Object> {

    // the only supported Map methods

    @Override
    public boolean isEmpty() {
        return super.isEmpty();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int size() {
        return super.size();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Object get(Object key) {
        return super.get(key);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void expungeClearedEntries() {

    }

    // unsupported methods (not needed for maintaining locks)

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
