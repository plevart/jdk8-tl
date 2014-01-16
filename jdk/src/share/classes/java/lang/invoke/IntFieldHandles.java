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
 * {@link #getVolatile}, {@link #setVolatile}, {@link #setOrdered}, {@link #getAndSet}, {@link #getAndAdd}
 * and {@link #compareAndSet}.
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
    public final MethodHandle getVolatile;
    /**
     * MethodHandle of type void(T, int) representing volatile field setter (volatile even if field is declared non-volatile)
     */
    public final MethodHandle setVolatile;
    /**
     * MethodHandle of type void(T, int) representing ordered field setter (like {@link AtomicInteger#lazySet})
     */
    public final MethodHandle setOrdered;
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

    // unsafe method handles...
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
//        MethodHandles.Lookup lookup = MethodHandles.Lookup.IMPL_LOOKUP;
        MethodHandles.Lookup lookup = MethodHandles.Lookup.IMPL_LOOKUP.in(IntFieldHandles.class);
        try {
            unsafeGetInt = lookup.findStatic(IntFieldHandles.class, "unsafeGetInt", methodType(int.class, Object.class, long.class));
            unsafePutInt = lookup.findStatic(IntFieldHandles.class, "unsafePutInt", methodType(void.class, Object.class, long.class, int.class));
            unsafeGetIntVolatile = lookup.findStatic(IntFieldHandles.class, "unsafeGetIntVolatile", methodType(int.class, Object.class, long.class));
            unsafePutIntVolatile = lookup.findStatic(IntFieldHandles.class, "unsafePutIntVolatile", methodType(void.class, Object.class, long.class, int.class));
            unsafePutOrderedInt = lookup.findStatic(IntFieldHandles.class, "unsafePutOrderedInt", methodType(void.class, Object.class, long.class, int.class));
            unsafeGetAndSetInt = lookup.findStatic(IntFieldHandles.class, "unsafeGetAndSetInt", methodType(int.class, Object.class, long.class, int.class));
            unsafeGetAndAddInt = lookup.findStatic(IntFieldHandles.class, "unsafeGetAndAddInt", methodType(int.class, Object.class, long.class, int.class));
            unsafeCompareAndSwapInt = lookup.findStatic(IntFieldHandles.class, "unsafeCompareAndSwapInt", methodType(boolean.class, Object.class, long.class, int.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private static int unsafeGetInt(Object o, long offset) {
        o.getClass(); // non-null check
        return unsafe.getInt(o, offset);
    }

    private static void unsafePutInt(Object o, long offset, int x) {
        o.getClass(); // non-null check
        unsafe.putInt(o, offset, x);
    }

    private static int unsafeGetIntVolatile(Object o, long offset) {
        o.getClass(); // non-null check
        return unsafe.getIntVolatile(o, offset);
    }

    private static void unsafePutIntVolatile(Object o, long offset, int x) {
        o.getClass(); // non-null check
        unsafe.putIntVolatile(o, offset, x);
    }

    private static void unsafePutOrderedInt(Object o, long offset, int x) {
        o.getClass(); // non-null check
        unsafe.putOrderedInt(o, offset, x);
    }

    private static int unsafeGetAndSetInt(Object o, long offset, int x) {
        o.getClass(); // non-null check
        return unsafe.getAndSetInt(o, offset, x);
    }

    private static int unsafeGetAndAddInt(Object o, long offset, int x) {
        o.getClass(); // non-null check
        return unsafe.getAndAddInt(o, offset, x);
    }

    private static boolean unsafeCompareAndSwapInt(Object o, long offset, int expected, int x) {
        o.getClass(); // non-null check
        return unsafe.compareAndSwapInt(o, offset, expected, x);
    }

    /**
     * Convenience factory method that calls constructor
     * {@link #IntFieldHandles(MethodHandles.Lookup, Class, String)}
     * using caller's {@link MethodHandles.Lookup} and caller's {@link Class}
     * as 1st and 2nd arguments respectively.
     *
     * @param fieldName the name of the field
     * @return an {@link IntFieldHandles} instance
     * @throws IllegalArgumentException wrapping any constructor thrown
     *                                  {@link IllegalAccessException} or
     *                                  {@link NoSuchFieldException}
     */
    @CallerSensitive
    public static IntFieldHandles intFieldHandles(String fieldName) {
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
     * @param lookup    the {@link MethodHandles.Lookup} to use for obtaining method handles for specified field
     * @param refc      the class or interface from which the field is accessed
     *                  (this will be the type of 1st parameter of all produced method handles)
     * @param fieldName the name of the field
     * @throws NoSuchFieldException   if the field does not exist
     * @throws IllegalAccessException if access checking fails, or if the field is static
     */
    public IntFieldHandles(MethodHandles.Lookup lookup, Class<?> refc, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        // obtain basic getter & setter MHs + perform access checks
        get = lookup.findGetter(refc, fieldName, int.class);
        set = lookup.findSetter(refc, fieldName, int.class);
        // reflect into Field
        MethodHandleInfo mhi = lookup.revealDirect(get);
        Field field = mhi.reflectAs(Field.class, lookup);
        // obtain offset
        long offset = unsafe.objectFieldOffset(field);
        // bind offset, change type of 1st parameter from Object to refc
        // this effectively makes returned MH safe...
        getRelaxed = bindOffsetAndChangeP0Type(unsafeGetInt, offset, refc);
        setRelaxed = bindOffsetAndChangeP0Type(unsafePutInt, offset, refc);
        getVolatile = bindOffsetAndChangeP0Type(unsafeGetIntVolatile, offset, refc);
        setVolatile = bindOffsetAndChangeP0Type(unsafePutIntVolatile, offset, refc);
        setOrdered = bindOffsetAndChangeP0Type(unsafePutOrderedInt, offset, refc);
        getAndSet = bindOffsetAndChangeP0Type(unsafeGetAndSetInt, offset, refc);
        getAndAdd = bindOffsetAndChangeP0Type(unsafeGetAndAddInt, offset, refc);
        compareAndSet = bindOffsetAndChangeP0Type(unsafeCompareAndSwapInt, offset, refc);
    }

    private static MethodHandle bindOffsetAndChangeP0Type(MethodHandle mh, long offset, Class<?> p0Type) {
        MethodHandle bmh = insertArguments(mh, 1, offset);
        return bmh.asType(bmh.type().changeParameterType(0, p0Type));
    }
}
