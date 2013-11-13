package sun.misc;

import java.util.function.Consumer;

/**
 * @author <peter.levart@gmail.com>
 */
public class CSUtil {

    private static final ThreadLocal<Class<?>> currentCaller = new ThreadLocal<>();

    public static void asCaller(Class<?> directCaller, Runnable task) {
        Class<?> caller = currentCaller.get();
        currentCaller.set(directCaller);
        try {
            task.run();
        } finally {
            currentCaller.set(caller);
        }
    }

    public static Consumer<Class<?>> asCallerInvoker(Runnable task) {
        return caller -> asCaller(caller, task);
    }

    public static void withCaller(Class<?> directCaller, Consumer<Class<?>> csTask) {
        Class<?> caller = currentCaller.get();
        if (caller == null) {
            csTask.accept(directCaller);
        } else {
            currentCaller.set(null);
            try {
                csTask.accept(caller);
            } finally {
                currentCaller.set(caller);
            }
        }
    }
}
