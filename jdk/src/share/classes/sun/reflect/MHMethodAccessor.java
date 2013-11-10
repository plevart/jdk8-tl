/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package sun.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author peter
 */
public class MHMethodAccessor implements MethodAccessor {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final MethodType methodAccessorInvokeType = MethodType.methodType(Object.class, Object.class, Object[].class);
    private static final MethodHandle throwInvocationTarget;
    private static final MethodHandle throwInvocationTargetOrNullPointer;

    static {
        try {
            throwInvocationTarget = lookup.findStatic(
                MHMethodAccessor.class,
                "throwInvocationTarget",
                MethodType.methodType(void.class, Throwable.class)
            );
            throwInvocationTargetOrNullPointer = lookup.findStatic(
                MHMethodAccessor.class,
                "throwInvocationTargetOrNullPointer",
                MethodType.methodType(void.class, Throwable.class, Object.class)
            );
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    // exception handler for static methods
    private static void throwInvocationTarget(Throwable targetException) throws InvocationTargetException {
        throw new InvocationTargetException(targetException);
    }

    // exception handler for instance methods
    private static void throwInvocationTargetOrNullPointer(Throwable targetException, Object target) throws InvocationTargetException {
        // in case an instance method was called with null target, we must not wrap the NPE in InvocationTargetException
        if (target == null) throw (NullPointerException) targetException;
        throw new InvocationTargetException(targetException);
    }

    // the adapted method handle
    private final MethodHandle mh;

    public MHMethodAccessor(Method method) {
        MethodHandle dmh;
        try {
            method.setAccessible(true); // TODO: use the almighty lookup instead
            dmh = lookup.unreflect(method);
        }
        catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }


        if (Modifier.isStatic(method.getModifiers())) {
            MethodHandle dmhThrowsIte = MethodHandles.catchException(
                dmh, Throwable.class,
                throwInvocationTarget.asType(MethodType.methodType(
                    dmh.type().returnType(), Throwable.class))
            );
            MethodHandle spreadInvoker = MethodHandles.spreadInvoker(dmhThrowsIte.type(), 0);
            MethodHandle boundSpreadInvoker = spreadInvoker.bindTo(dmhThrowsIte);
            boundSpreadInvoker = MethodHandles.dropArguments(boundSpreadInvoker, 0, Object.class);
            mh = boundSpreadInvoker.asType(methodAccessorInvokeType);
        } else {
            MethodHandle dmhThrowsIteOrNpe = MethodHandles.catchException(
                dmh, Throwable.class,
                throwInvocationTargetOrNullPointer.asType(MethodType.methodType(
                    dmh.type().returnType(), Throwable.class, dmh.type().parameterType(0)))
            );
            MethodHandle spreadInvoker = MethodHandles.spreadInvoker(dmhThrowsIteOrNpe.type(), 1);
            MethodHandle boundSpreadInvoker = spreadInvoker.bindTo(dmhThrowsIteOrNpe);
            mh = boundSpreadInvoker.asType(methodAccessorInvokeType);
        }
    }

    @Override
    public Object invoke(Object target, Object[] args) throws IllegalArgumentException, InvocationTargetException {
        try {
            return mh.invokeExact(target, args);
        }
        catch (IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            throw e;
        }
        catch (ClassCastException e) {
            throw new IllegalArgumentException("target or argument type mismatch", e);
        }
        catch (Throwable e) {
            throw new AssertionError("Should not happen", e);
        }
    }
}
