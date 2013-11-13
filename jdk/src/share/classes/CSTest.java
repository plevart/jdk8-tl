import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import static sun.misc.CSUtil.asCaller;
import static sun.misc.CSUtil.asCallerInvoker;
import static sun.misc.CSUtil.withCaller;

/**
 * @author <peter.levart@gmail.com>
 */
public class CSTest {

    @CallerSensitive
    static void backMethod(String s) { withCaller(Reflection.getCallerClass(), caller -> {

            System.out.println(s + " -> backMethod(caller = " + caller.getSimpleName() + ")");

    });}

    static class Infrastructure {
        static void midMethod(String s) {
            backMethod(s + " -> Infrastructure.midMethod");
        }
    }

    @CallerSensitive
    static void frontMethod1(String s) { withCaller(Reflection.getCallerClass(), caller -> {

            Infrastructure.midMethod(s + " -> frontMethod1:pre");

            asCaller(caller, () -> {
                Infrastructure.midMethod(s + " -> frontMethod1:asCaller");
            });

            Infrastructure.midMethod(s + " -> frontMethod1:post");

    });}

    @CallerSensitive
    static void frontMethod2(String s) { withCaller(Reflection.getCallerClass(), asCallerInvoker(() -> {

                Infrastructure.midMethod(s + " -> frontMethod2:asCallerInvoker");

    }));}

    static class Client {
        static void test() {
            backMethod("Client.test");
            frontMethod1("Client.test");
            frontMethod2("Client.test");
        }
    }

    public static void main(String[] args) {
        Client.test();
    }
}
