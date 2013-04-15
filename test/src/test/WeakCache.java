/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

/**
 * Cache mapping pairs of {@code (key, sub-key) -> value}. Keys and values are weakly but sub-keys are strongly referenced.
 * Keys are passed directly to {@link #get} method which also takes a {@code parameter}. Sub-keys are calculated from
 * keys and parameters using the {@code subKeyFactory} function passed to the constructor. Values are
 * calculated from keys and parameters using the {@code valueFactory} function passed to the constructor.
 * Keys can be null and are compared by identity while sub-keys returned by {@code subKeyFactory} or
 * values returned by {@code valueFactory} can not be null.
 * Entries are expunged from cache lazily on each invocation to {@link #get} method.
 *
 * @param <K> type of keys
 * @param <P> type of parameters
 * @param <V> type of values
 */
public interface WeakCache<K, P, V>
{
    /**
     * Look-up the value through the cache. This always evaluates the {@code subKeyFactory} function
     * and optionally evaluates {@code valueFactory} function if there is no entry in the cache for given pair of
     * (key, subKey) or the entry has already been cleared.
     *
     * @param key       possibly null key
     * @param parameter parameter used together with key to create sub-key and value (should not be null)
     * @return the cached value (never null)
     * @throws NullPointerException if {@code parameter} passed in or {@code sub-key} calculated by
     *                              {@code subKeyFactory} or {@code value} calculated by {@code valueFactory} is null.
     * @throws RuntimeException     or subtype if {@code subKeyFactory} or {@code valueFactory} throws it
     * @throws Error                or subtype if {@code subKeyFactory} or {@code valueFactory} throws it
     */
    V get(K key, P parameter);

    /**
     * Optional operation.
     * Checks whether the specified non-null value is already present in this {@code WeakCache}. The check is made
     * by value's reference regardless of whether it's class overrides {@link Object#equals} or not.
     * @param value the non-null value to check
     * @return true if given {@code value} is already cached
     * @throws UnsupportedOperationException is this operation is not supported in this instance of cache
     */
    boolean containsValue(V value);
}
