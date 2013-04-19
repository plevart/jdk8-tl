/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

import java.lang.reflect.Method;

/**
 * @author peter
 */
public class FindLoadedClassTest {

    static final ClassLoader cl0 = FindLoadedClassTest.class.getClassLoader().getParent();
    static final ClassLoader cl1 = FindLoadedClassTest.class.getClassLoader();
    static final ClassLoader cl2 = new ClassLoader(cl1) {};

    static void test(Class c) throws Exception {

        Method findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass0", String.class);
        findLoadedClassMethod.setAccessible(true);

        Class c0 = (Class) findLoadedClassMethod.invoke(cl0, c.getName());
        Class c1 = (Class) findLoadedClassMethod.invoke(cl1, c.getName());
        Class c2 = (Class) findLoadedClassMethod.invoke(cl2, c.getName());

        System.out.println("\n*** " + c.getName() + "\n");
        System.out.println(cl0 + ": " + c0);
        System.out.println(cl1 + ": " + c1);
        System.out.println(cl2 + ": " + c2);
        System.out.println(c.getClassLoader());

    }

    public static void main(String[] args) throws Exception {
        test(FindLoadedClassTest.class);
        test(Runnable.class);

        Class.forName(Runnable.class.getName(), false, cl0);
        test(Runnable.class);

        Class.forName(Runnable.class.getName(), false, cl2);
        test(Runnable.class);

    }
}
