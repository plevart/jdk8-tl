/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package java.lang.reflect;

import sun.misc.VM;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * @author peter
 */
class ClassFinder {
    private static final MethodHandle findLoadedClass0MH, findBootstrapClassMH;
    private static final ClassLoader dummyCL = new ClassLoader() {};

    static {
        try {
            Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass0", String.class);
            method.setAccessible(true);
            findLoadedClass0MH = MethodHandles.lookup().unreflect(method);

            method = ClassLoader.class.getDeclaredMethod("findBootstrapClass", String.class);
            method.setAccessible(true);
            findBootstrapClassMH = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException e) {
            throw (Error) new NoSuchMethodError(e.getMessage()).initCause(e);
        } catch (IllegalAccessException e) {
            throw (Error) new IllegalAccessError(e.getMessage()).initCause(e);
        }
    }

    static Class<?> findLoadedClass(ClassLoader loader, String name) {
        try {
            if (VM.isSystemDomainLoader(loader)) {
                return (Class<?>) findBootstrapClassMH.invokeExact(dummyCL, name);
            } else {
                return (Class<?>) findLoadedClass0MH.invokeExact(loader, name);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }
}
