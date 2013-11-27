/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package sun.misc;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * @author peter
 */
public class Caller implements AutoCloseable {
    private static final ThreadLocal<Caller> currentCaller = new ThreadLocal<>();

    @CallerSensitive
    public static Caller as(Class<?> transitiveCallerClass) {
        Caller caller = new Caller(Reflection.getCallerClass(), transitiveCallerClass, currentCaller.get());
        currentCaller.set(caller);
        return caller;
    }

    public static Class<?> get(Class<?> directCallerClass) {
        Caller caller = currentCaller.get();
        return caller != null && caller.directCallerClass == directCallerClass
               ? caller.transitiveCallerClass
               : directCallerClass;
    }

    private final Class<?> directCallerClass;
    private final Class<?> transitiveCallerClass;
    private Caller prev;

    private Caller(Class<?> directCallerClass, Class<?> transitiveCallerClass, Caller prev) {
        this.directCallerClass = directCallerClass;
        this.transitiveCallerClass = transitiveCallerClass;
    }

    @Override
    public void close() {
        if (prev != this) {
            currentCaller.set(prev);
            prev = this;
        }
    }
}
