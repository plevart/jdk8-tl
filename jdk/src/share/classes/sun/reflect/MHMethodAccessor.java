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
import static java.lang.invoke.MethodType.methodType;

/**
 * {@link MethodAccessor} implementations based on {@link MethodHandle}s
 *
 * @author Peter.Levart@gmail.com
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
            // direct method handle of the target method
            dmh = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            // should not happen since the almighty lookup is used
            throw new AssertionError(e);
        }

        int paramCount = dmh.type().parameterCount();

        if (Modifier.isStatic(method.getModifiers())) {
            // catch any Throwable and wrap it in the InvocationTargetException
            MethodHandle throwsIte = catchException(dmh, Throwable.class,
                throwInvocationTarget.asType(methodType(
                    dmh.type().returnType(), Throwable.class))
            );
            // MHs for static methods don't have the leading target argument
            // so we introduce one which is ignored when called
            // R m(P1 p1, P2 p2, ...) -> R m(Object ignored, Object[] args_p1_p2_etc)
            MethodHandle spreader = dropArguments(
                throwsIte.asSpreader(Object[].class, paramCount),
                0, Object.class
            );
            // adapt return type
            // R m(Object ignored, Object[] args) -> Object m(Object ignored, Object[] args)
            mh = spreader.asType(MethodAccessor_invoke_type);
        } else { // instance method
            // catch any Throwable and wrap it in the InvocationTargetException
            // unless it is a NullPointerException caused by null target
            MethodHandle throwsIteOrNpe = catchException(dmh, Throwable.class,
                throwInvocationTargetOrNullPointer.asType(methodType(
                    dmh.type().returnType(), Throwable.class,
                    dmh.type().parameterType(0)))
            );
            // leave the target argument alone, spread the rest
            // R m(T target, P1 p1, P2 p2, ...) -> R m(T target, Object[] args_p1_p2_etc)
            MethodHandle spreader =
                throwsIteOrNpe.asSpreader(Object[].class, paramCount - 1);
            // adapt return type and target argument type
            // R m(T target, Object[] args) -> Object m(Object target, Object[] args)
            mh = spreader.asType(MethodAccessor_invoke_type);
        }
    }

    @Override
    public Object invoke(Object target, Object[] args)
        throws IllegalArgumentException, InvocationTargetException {
        try {
            return mh.invokeExact(target, args);
        } catch (IllegalArgumentException // thrown by MH adapters on argument count mismatch
            | InvocationTargetException   // wrapped target exception
            | NullPointerException e) {   // in case of instance method when target was null
            // re-throw exceptions that are already of correct type
            throw e;
        } catch (ClassCastException e) {  // thrown by MH adapters on target or argument type mismatch
            // convert to IllegalArgumentException
            throw new IllegalArgumentException("target or argument type mismatch", e);
        } catch (Throwable e) {
            // any other Throwable type besides those above is a bug
            throw new AssertionError("Should not happen", e);
        }
    }
}
