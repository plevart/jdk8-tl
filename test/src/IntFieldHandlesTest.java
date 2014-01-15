/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.invoke.IntFieldHandles;

import static java.lang.invoke.IntFieldHandles.fieldHandles;

/**
 * @author peter.levart@gmail.com
 */
public class IntFieldHandlesTest {
    private int x;
    private static final IntFieldHandles X = fieldHandles("x");

    public static void main(String[] args) throws Throwable {
        IntFieldHandlesTest t = new IntFieldHandlesTest();
        t.x = 12;
        System.out.println((int) X.getAcquire.invokeExact(t));

        X.setRelease.invokeExact(t, 13);
        System.out.println(t.x);

        System.out.println((boolean) X.compareAndSet.invokeExact(t, 13, 14));
        System.out.println((int) X.get.invokeExact(t));

        System.out.println((boolean) X.compareAndSet.invokeExact(t, 13, 15));
        System.out.println((int) X.get.invokeExact(t));
    }
}
