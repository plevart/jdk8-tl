/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.misc.Caller;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * @author peter
 */
public class CallerTest {

    static class SystemUtility {
        @CallerSensitive
        static void m(String path) {
            Class<?> caller = Caller.get(Reflection.getCallerClass());
            java.lang.System.out.println(path + " -> SystemUtility{Caller.get=" + caller.getSimpleName() + "}");
        }
    }

    static class Backend {
        @CallerSensitive
        static void m(String path) {
            Class<?> caller = Caller.get(Reflection.getCallerClass());
            SystemUtility.m(path + " -> Backend{Caller.get=" + caller.getSimpleName() + "}");
        }
    }

    static class Middle {
        static void m(String path, Class<?> callerClass) {
            try (Caller as = Caller.as(callerClass)){
                Backend.m(path + " -> Middle{Caller.as(" + callerClass.getSimpleName() + ")}");
            }
        }
    }

    static class Frontend {
        @CallerSensitive
        static void m(String path) {
            Class<?> caller = Caller.get(Reflection.getCallerClass());
            Middle.m(path + " -> Frontend{Caller.get=" + caller.getSimpleName() + "}", caller);
        }
    }

    static class Client {
        static void m() {
            SystemUtility.m("Client");
            Backend.m("Client");
            Frontend.m("Client");
        }
    }

    public static void main(String[] args) {
        Client.m();
    }
}
