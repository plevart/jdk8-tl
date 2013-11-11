/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package sun.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandles.catchException;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.spreadInvoker;
import static java.lang.invoke.MethodType.methodType;

/**
 * @author peter
 */
public class MHMethodAccessor implements MethodAccessor {

    private static final MethodHandles.Lookup lookup;
    private static final MethodType MethodAccessor_invoke_type =
        methodType(Object.class, Object.class, Object[].class);
    private static final MethodHandle throwInvocationTarget;
    private static final MethodHandle throwInvocationTargetOrNullPointer;

    static {
        try {
            Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            f.setAccessible(true);
            lookup = (MethodHandles.Lookup) f.get(null);

            throwInvocationTarget = lookup.findStatic(
                MHMethodAccessor.class,
                "throwInvocationTarget",
                methodType(void.class, Throwable.class)
            );

            throwInvocationTargetOrNullPointer = lookup.findStatic(
                MHMethodAccessor.class,
                "throwInvocationTargetOrNullPointer",
                methodType(void.class, Throwable.class, Object.class)
            );
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    // exception handler for static methods
    private static void throwInvocationTarget(Throwable targetException)
        throws InvocationTargetException {
        throw new InvocationTargetException(targetException);
    }

    // exception handler for instance methods
    private static void throwInvocationTargetOrNullPointer(Throwable targetException,
                                                           Object target)
        throws InvocationTargetException {
        // in case an instance method was called with null target,
        // we must not wrap the NPE in InvocationTargetException
        if (target == null) throw (NullPointerException) targetException;
        throw new InvocationTargetException(targetException);
    }

    // the adapted method handle
    private final MethodHandle mh;

    public MHMethodAccessor(Method method) {
        MethodHandle dmh;
        try {
            dmh = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }

        if (Modifier.isStatic(method.getModifiers())) {
            MethodHandle throwsIte = catchException(dmh, Throwable.class,
                throwInvocationTarget.asType(methodType(
                    dmh.type().returnType(), Throwable.class))
            );
            MethodHandle spreader = dropArguments(
                spreadInvoker(throwsIte.type(), 0).bindTo(throwsIte),
                0, Object.class
            );
            mh = spreader.asType(MethodAccessor_invoke_type);
        } else {
            MethodHandle throwsIteOrNpe = catchException(dmh, Throwable.class,
                throwInvocationTargetOrNullPointer.asType(methodType(
                    dmh.type().returnType(), Throwable.class,
                    dmh.type().parameterType(0)))
            );
            MethodHandle spreader =
                spreadInvoker(throwsIteOrNpe.type(), 1).bindTo(throwsIteOrNpe);
            mh = spreader.asType(MethodAccessor_invoke_type);
        }
    }

    @Override
    public Object invoke(Object target, Object[] args)
        throws IllegalArgumentException, InvocationTargetException {
        try {
            return mh.invokeExact(target, args);
        } catch (IllegalArgumentException | InvocationTargetException | NullPointerException e) {
            throw e;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("target or argument type mismatch", e);
        } catch (Throwable e) {
            throw new AssertionError("Should not happen", e);
        }
    }

    @CallerSensitive
    public static Class<?> getCaller() {
        return Reflection.getCallerClass();
    }
}
