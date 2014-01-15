/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package java.lang.invoke;

import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

/**
 * A factory and container of {@link MethodHandle}s
 * which can be used for accessing and executing atomic operations on a specific instance {@code int} field.<p>
 * The method handles are: {@link #get}, {@link #set}, {@link #getRelaxed}, {@link #setRelaxed},
 * {@link #getAcquire}, {@link #setRelease}, {@link #setSequential}, {@link #getAndSet}, {@link #getAndAdd}
 * and {@link #compareAndSet}.
 *
 * @author peter.levart@gmail.com
 */
public final class IntFieldHandles {
    /**
     * MethodHandle of type int(T) representing basic field getter (respecting volatile/non-volatile declaration of field)
     */
    public final MethodHandle get;
    /**
     * MethodHandle of type void(T, int) representing basic field setter (respecting volatile/non-volatile declaration of field)
     */
    public final MethodHandle set;
    /**
     * MethodHandle of type int(T) representing relaxed field getter (non-volatile even if field is declared volatile)
     */
    public final MethodHandle getRelaxed;
    /**
     * MethodHandle of type void(T, int) representing relaxed field setter (non-volatile even if field is declared volatile)
     */
    public final MethodHandle setRelaxed;
    /**
     * MethodHandle of type int(T) representing volatile field getter (volatile even if field is declared non-volatile)
     */
    public final MethodHandle getAcquire;
    /**
     * MethodHandle of type void(T, int) representing volatile field setter (volatile even if field is declared non-volatile)
     */
    public final MethodHandle setRelease;
    /**
     * MethodHandle of type void(T, int) representing ordered field setter (like {@link AtomicInteger#lazySet})
     */
    public final MethodHandle setSequential;
    /**
     * MethodHandle of type int(T, int) representing atomic get-and-set operation (like {@link AtomicInteger#getAndSet})
     */
    public final MethodHandle getAndSet;
    /**
     * MethodHandle of type int(T, int) representing atomic get-and-add operation (like {@link AtomicInteger#getAndAdd})
     */
    public final MethodHandle getAndAdd;
    /**
     * MethodHandle of type boolean(T, int, int) representing atomic compare-and-set operation (like {@link AtomicInteger#compareAndSet})
     */
    public final MethodHandle compareAndSet;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final MethodHandle unsafeGetInt;
    private static final MethodHandle unsafePutInt;
    private static final MethodHandle unsafeGetIntVolatile;
    private static final MethodHandle unsafePutIntVolatile;
    private static final MethodHandle unsafePutOrderedInt;
    private static final MethodHandle unsafeGetAndSetInt;
    private static final MethodHandle unsafeGetAndAddInt;
    private static final MethodHandle unsafeCompareAndSwapInt;

    static {
        MethodHandles.Lookup lookup = MethodHandles.Lookup.IMPL_LOOKUP;
        try {
            unsafeGetInt = lookup.findVirtual(Unsafe.class, "getInt", methodType(int.class, Object.class, long.class)).bindTo(unsafe);
            unsafePutInt = lookup.findVirtual(Unsafe.class, "putInt", methodType(void.class, Object.class, long.class, int.class)).bindTo(unsafe);
            unsafeGetIntVolatile = lookup.findVirtual(Unsafe.class, "getIntVolatile", methodType(int.class, Object.class, long.class)).bindTo(unsafe);
            unsafePutIntVolatile = lookup.findVirtual(Unsafe.class, "putIntVolatile", methodType(void.class, Object.class, long.class, int.class)).bindTo(unsafe);
            unsafePutOrderedInt = lookup.findVirtual(Unsafe.class, "putOrderedInt", methodType(void.class, Object.class, long.class, int.class)).bindTo(unsafe);
            unsafeGetAndSetInt = lookup.findVirtual(Unsafe.class, "getAndSetInt", methodType(int.class, Object.class, long.class, int.class)).bindTo(unsafe);
            unsafeGetAndAddInt = lookup.findVirtual(Unsafe.class, "getAndAddInt", methodType(int.class, Object.class, long.class, int.class)).bindTo(unsafe);
            unsafeCompareAndSwapInt = lookup.findVirtual(Unsafe.class, "compareAndSwapInt", methodType(boolean.class, Object.class, long.class, int.class, int.class)).bindTo(unsafe);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * Convenience method that calls constructor {@link #IntFieldHandles(MethodHandles.Lookup, Class, String)}
     * using caller's lookup and caller's class as 1st and 2nd arguments respectively.
     *
     * @param fieldName the name of the field
     * @return a {@link IntFieldHandles} instance
     * @throws IllegalArgumentException if constructor throws a checked exception
     */
    @CallerSensitive
    public static IntFieldHandles fieldHandles(String fieldName) {
        Class<?> cc = Reflection.getCallerClass();
        MethodHandles.Lookup lookup = MethodHandles.Lookup.IMPL_LOOKUP.in(cc);
        try {
            return new IntFieldHandles(lookup, cc, fieldName);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Constructs new instance of {@link IntFieldHandles}.
     *
     * @param lookup    the {@link MethodHandles.Lookup} to use to obtain method handles for specified field
     * @param refc      the class or interface from which the field will be accessed
     *                  (this will be the 1st argument type of all produced method handles)
     * @param fieldName the name of the field
     * @throws NoSuchFieldException   if the field does not exist
     * @throws IllegalAccessException if access checking fails, or if the field is static
     */
    public IntFieldHandles(MethodHandles.Lookup lookup, Class<?> refc, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        // obtain basic getter & setter MHs + perform access checks
        get = lookup.findGetter(refc, fieldName, int.class);
        set = lookup.findSetter(refc, fieldName, int.class);
        MethodHandleInfo mhi = lookup.revealDirect(get);
        Field field = mhi.reflectAs(Field.class, lookup);
        long offset = unsafe.objectFieldOffset(field);
        getRelaxed = insertArguments(unsafeGetInt, 1, offset).asType(methodType(int.class, refc));
        setRelaxed = insertArguments(unsafePutInt, 1, offset).asType(methodType(void.class, refc, int.class));
        getAcquire = insertArguments(unsafeGetIntVolatile, 1, offset).asType(methodType(int.class, refc));
        setRelease = insertArguments(unsafePutIntVolatile, 1, offset).asType(methodType(void.class, refc, int.class));
        setSequential = insertArguments(unsafePutOrderedInt, 1, offset).asType(methodType(void.class, refc, int.class));
        getAndSet = insertArguments(unsafeGetAndSetInt, 1, offset).asType(methodType(int.class, refc, int.class));
        getAndAdd = insertArguments(unsafeGetAndAddInt, 1, offset).asType(methodType(int.class, refc, int.class));
        compareAndSet = insertArguments(unsafeCompareAndSwapInt, 1, offset).asType(methodType(boolean.class, refc, int.class, int.class));
    }
}
