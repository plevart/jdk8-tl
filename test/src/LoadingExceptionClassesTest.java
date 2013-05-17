/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * @author peter
 */
public class LoadingExceptionClassesTest {

    private static class Handler {
        static {
            System.out.println(Handler.class.getName() + " executing static initializer");
        }

        Object lock = new Object();

        void m() {
            synchronized (lock) {
                try {
                    if (lock != null) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {}
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // wait for system background threads to load their classes
        Thread.sleep(1000L);
        System.out.println();
        System.out.println("START!");
        System.out.println();

        String handlerClassName = LoadingExceptionClassesTest.class.getName() + "$Handler";

        System.out.println(handlerClassName + " loading");
        Class.forName(handlerClassName, false, LoadingExceptionClassesTest.class.getClassLoader());
        System.out.println(handlerClassName + " loaded");

        System.out.println(handlerClassName + " initializing");
        Class.forName(handlerClassName, true, LoadingExceptionClassesTest.class.getClassLoader());
        System.out.println(handlerClassName + " initialized");

        System.out.println("constructing instance of " + handlerClassName);
        Handler h = new Handler();
        System.out.println("constructed " + h);

        System.out.println("calling " + handlerClassName + ".m()");
        Thread.currentThread().interrupt();
        h.m();
        System.out.println("called " + handlerClassName + ".m()");
    }
}
