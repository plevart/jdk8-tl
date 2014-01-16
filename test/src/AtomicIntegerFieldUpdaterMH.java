/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Abstract base for specialized subclasses that provide constant method handles for
 * various atomic operations on specific field.
 */
public abstract class AtomicIntegerFieldUpdaterMH<T> extends AtomicIntegerFieldUpdater<T> {

    // make MH generic by changing 1st parameter type from whatever it is to erased T (Object)
    // this inserts a checkcast operation...
    static MethodHandle makeGeneric(MethodHandle mh) {
        return mh.asType(mh.type().changeParameterType(0, Object.class));
    }

    abstract MethodHandle getVolatileMH();
    abstract MethodHandle setVolatileMH();
    abstract MethodHandle setOrderedMH();
    abstract MethodHandle getAndSetMH();
    abstract MethodHandle getAndAddMH();
    abstract MethodHandle compareAndSetMH();

    @Override
    public int get(T obj) {
        try {
            return (int) getVolatileMH().invokeExact(obj);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public void set(T obj, int newValue) {
        try {
            setVolatileMH().invokeExact(obj, newValue);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public void lazySet(T obj, int newValue) {
        try {
            setOrderedMH().invokeExact(obj, newValue);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public int getAndSet(T obj, int newValue) {
        try {
            return (int) getAndSetMH().invokeExact(obj, newValue);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public int getAndAdd(T obj, int delta) {
        try {
            return (int) getAndAddMH().invokeExact(obj, delta);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public boolean compareAndSet(T obj, int expect, int update) {
        try {
            return (boolean) compareAndSetMH().invokeExact(obj, expect, update);
        } catch (Throwable t) { throw new AssertionError(t); }
    }

    @Override
    public boolean weakCompareAndSet(T obj, int expect, int update) {
        try {
            return (boolean) compareAndSetMH().invokeExact(obj, expect, update);
        } catch (Throwable t) { throw new AssertionError(t); }
    }
}
