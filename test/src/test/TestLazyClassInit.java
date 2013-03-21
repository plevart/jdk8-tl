/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

/**
 * @author peter
 */
public class TestLazyClassInit {

    private static final boolean FALSE = Boolean.getBoolean("non.existent.property");

    static class Foo {
        static {
            System.out.println("Foo initialized");
        }

        static boolean isOk() {
            return true;
        }
    }

    public static void main(String[] args) {
        if (FALSE && Foo.isOk()) {
            System.out.println("FALSE is true");
        } else {
            System.out.println("FALSE is false");
        }
    }
}
