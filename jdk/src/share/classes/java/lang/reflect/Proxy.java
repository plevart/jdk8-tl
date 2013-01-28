/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.reflect;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import sun.misc.ProxyGenerator;
import sun.misc.Unsafe;

/**
 * {@code Proxy} provides static methods for creating dynamic proxy
 * classes and instances, and it is also the superclass of all
 * dynamic proxy classes created by those methods.
 *
 * <p>To create a proxy for some interface {@code Foo}:
 * <pre>
 *     InvocationHandler handler = new MyInvocationHandler(...);
 *     Class proxyClass = Proxy.getProxyClass(
 *         Foo.class.getClassLoader(), new Class[] { Foo.class });
 *     Foo f = (Foo) proxyClass.
 *         getConstructor(new Class[] { InvocationHandler.class }).
 *         newInstance(new Object[] { handler });
 * </pre>
 * or more simply:
 * <pre>
 *     Foo f = (Foo) Proxy.newProxyInstance(Foo.class.getClassLoader(),
 *                                          new Class[] { Foo.class },
 *                                          handler);
 * </pre>
 *
 * <p>A <i>dynamic proxy class</i> (simply referred to as a <i>proxy
 * class</i> below) is a class that implements a list of interfaces
 * specified at runtime when the class is created, with behavior as
 * described below.
 *
 * A <i>proxy interface</i> is such an interface that is implemented
 * by a proxy class.
 *
 * A <i>proxy instance</i> is an instance of a proxy class.
 *
 * Each proxy instance has an associated <i>invocation handler</i>
 * object, which implements the interface {@link InvocationHandler}.
 * A method invocation on a proxy instance through one of its proxy
 * interfaces will be dispatched to the {@link InvocationHandler#invoke
 * invoke} method of the instance's invocation handler, passing the proxy
 * instance, a {@code java.lang.reflect.Method} object identifying
 * the method that was invoked, and an array of type {@code Object}
 * containing the arguments.  The invocation handler processes the
 * encoded method invocation as appropriate and the result that it
 * returns will be returned as the result of the method invocation on
 * the proxy instance.
 *
 * <p>A proxy class has the following properties:
 *
 * <ul>
 * <li>Proxy classes are public, final, and not abstract.
 *
 * <li>The unqualified name of a proxy class is unspecified.  The space
 * of class names that begin with the string {@code "$Proxy"}
 * should be, however, reserved for proxy classes.
 *
 * <li>A proxy class extends {@code java.lang.reflect.Proxy}.
 *
 * <li>A proxy class implements exactly the interfaces specified at its
 * creation, in the same order.
 *
 * <li>If a proxy class implements a non-public interface, then it will
 * be defined in the same package as that interface.  Otherwise, the
 * package of a proxy class is also unspecified.  Note that package
 * sealing will not prevent a proxy class from being successfully defined
 * in a particular package at runtime, and neither will classes already
 * defined by the same class loader and the same package with particular
 * signers.
 *
 * <li>Since a proxy class implements all of the interfaces specified at
 * its creation, invoking {@code getInterfaces} on its
 * {@code Class} object will return an array containing the same
 * list of interfaces (in the order specified at its creation), invoking
 * {@code getMethods} on its {@code Class} object will return
 * an array of {@code Method} objects that include all of the
 * methods in those interfaces, and invoking {@code getMethod} will
 * find methods in the proxy interfaces as would be expected.
 *
 * <li>The {@link Proxy#isProxyClass Proxy.isProxyClass} method will
 * return true if it is passed a proxy class-- a class returned by
 * {@code Proxy.getProxyClass} or the class of an object returned by
 * {@code Proxy.newProxyInstance}-- and false otherwise.
 *
 * <li>The {@code java.security.ProtectionDomain} of a proxy class
 * is the same as that of system classes loaded by the bootstrap class
 * loader, such as {@code java.lang.Object}, because the code for a
 * proxy class is generated by trusted system code.  This protection
 * domain will typically be granted
 * {@code java.security.AllPermission}.
 *
 * <li>Each proxy class has one public constructor that takes one argument,
 * an implementation of the interface {@link InvocationHandler}, to set
 * the invocation handler for a proxy instance.  Rather than having to use
 * the reflection API to access the public constructor, a proxy instance
 * can be also be created by calling the {@link Proxy#newProxyInstance
 * Proxy.newProxyInstance} method, which combines the actions of calling
 * {@link Proxy#getProxyClass Proxy.getProxyClass} with invoking the
 * constructor with an invocation handler.
 * </ul>
 *
 * <p>A proxy instance has the following properties:
 *
 * <ul>
 * <li>Given a proxy instance {@code proxy} and one of the
 * interfaces implemented by its proxy class {@code Foo}, the
 * following expression will return true:
 * <pre>
 *     {@code proxy instanceof Foo}
 * </pre>
 * and the following cast operation will succeed (rather than throwing
 * a {@code ClassCastException}):
 * <pre>
 *     {@code (Foo) proxy}
 * </pre>
 *
 * <li>Each proxy instance has an associated invocation handler, the one
 * that was passed to its constructor.  The static
 * {@link Proxy#getInvocationHandler Proxy.getInvocationHandler} method
 * will return the invocation handler associated with the proxy instance
 * passed as its argument.
 *
 * <li>An interface method invocation on a proxy instance will be
 * encoded and dispatched to the invocation handler's {@link
 * InvocationHandler#invoke invoke} method as described in the
 * documentation for that method.
 *
 * <li>An invocation of the {@code hashCode},
 * {@code equals}, or {@code toString} methods declared in
 * {@code java.lang.Object} on a proxy instance will be encoded and
 * dispatched to the invocation handler's {@code invoke} method in
 * the same manner as interface method invocations are encoded and
 * dispatched, as described above.  The declaring class of the
 * {@code Method} object passed to {@code invoke} will be
 * {@code java.lang.Object}.  Other public methods of a proxy
 * instance inherited from {@code java.lang.Object} are not
 * overridden by a proxy class, so invocations of those methods behave
 * like they do for instances of {@code java.lang.Object}.
 * </ul>
 *
 * <h3>Methods Duplicated in Multiple Proxy Interfaces</h3>
 *
 * <p>When two or more interfaces of a proxy class contain a method with
 * the same name and parameter signature, the order of the proxy class's
 * interfaces becomes significant.  When such a <i>duplicate method</i>
 * is invoked on a proxy instance, the {@code Method} object passed
 * to the invocation handler will not necessarily be the one whose
 * declaring class is assignable from the reference type of the interface
 * that the proxy's method was invoked through.  This limitation exists
 * because the corresponding method implementation in the generated proxy
 * class cannot determine which interface it was invoked through.
 * Therefore, when a duplicate method is invoked on a proxy instance,
 * the {@code Method} object for the method in the foremost interface
 * that contains the method (either directly or inherited through a
 * superinterface) in the proxy class's list of interfaces is passed to
 * the invocation handler's {@code invoke} method, regardless of the
 * reference type through which the method invocation occurred.
 *
 * <p>If a proxy interface contains a method with the same name and
 * parameter signature as the {@code hashCode}, {@code equals},
 * or {@code toString} methods of {@code java.lang.Object},
 * when such a method is invoked on a proxy instance, the
 * {@code Method} object passed to the invocation handler will have
 * {@code java.lang.Object} as its declaring class.  In other words,
 * the public, non-final methods of {@code java.lang.Object}
 * logically precede all of the proxy interfaces for the determination of
 * which {@code Method} object to pass to the invocation handler.
 *
 * <p>Note also that when a duplicate method is dispatched to an
 * invocation handler, the {@code invoke} method may only throw
 * checked exception types that are assignable to one of the exception
 * types in the {@code throws} clause of the method in <i>all</i> of
 * the proxy interfaces that it can be invoked through.  If the
 * {@code invoke} method throws a checked exception that is not
 * assignable to any of the exception types declared by the method in one
 * of the proxy interfaces that it can be invoked through, then an
 * unchecked {@code UndeclaredThrowableException} will be thrown by
 * the invocation on the proxy instance.  This restriction means that not
 * all of the exception types returned by invoking
 * {@code getExceptionTypes} on the {@code Method} object
 * passed to the {@code invoke} method can necessarily be thrown
 * successfully by the {@code invoke} method.
 *
 * @author      Peter Jones
 * @see         InvocationHandler
 * @since       1.3
 */
public class Proxy implements java.io.Serializable {

    private static final long serialVersionUID = -2222568056686623797L;

    /** parameter types of a proxy class constructor */
    private final static Class[] constructorParams =
        { InvocationHandler.class };

    /** cache for proxy classes defined by bootstrap class loader */
    private final static ConcurrentMap<Object, Supplier<Class<?>>>
        bootstrapCLProxyClassCache = new ConcurrentHashMap<>();

    /** thread local context for initialization of isProxyClass ClassValue */
    final static ThreadLocal<Boolean> proxyClassInConstruction =
        new ThreadLocal<Boolean>() {
            /**
             * Will only be invoked in a rare occasion where isProxyClass is
             * evaluated for a subclass of j.l.r.Proxy that is not a proxy class.
             */
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

    /** isProxyClass ClassValue */
    final static ClassValue<Boolean> isProxyClass =
        new ClassValue<Boolean>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                return proxyClassInConstruction.get();
            }
        };

    /**
     * the invocation handler for this proxy instance.
     * @serial
     */
    protected InvocationHandler h;

    /**
     * Prohibits instantiation.
     */
    private Proxy() {
    }

    /**
     * Constructs a new {@code Proxy} instance from a subclass
     * (typically, a dynamic proxy class) with the specified value
     * for its invocation handler.
     *
     * @param   h the invocation handler for this proxy instance
     */
    protected Proxy(InvocationHandler h) {
        this.h = h;
    }

    /**
     * Returns the {@code java.lang.Class} object for a proxy class
     * given a class loader and an array of interfaces.  The proxy class
     * will be defined by the specified class loader and will implement
     * all of the supplied interfaces.  If a proxy class for the same
     * permutation of interfaces has already been defined by the class
     * loader, then the existing proxy class will be returned; otherwise,
     * a proxy class for those interfaces will be generated dynamically
     * and defined by the class loader.
     *
     * <p>There are several restrictions on the parameters that may be
     * passed to {@code Proxy.getProxyClass}:
     *
     * <ul>
     * <li>All of the {@code Class} objects in the
     * {@code interfaces} array must represent interfaces, not
     * classes or primitive types.
     *
     * <li>No two elements in the {@code interfaces} array may
     * refer to identical {@code Class} objects.
     *
     * <li>All of the interface types must be visible by name through the
     * specified class loader.  In other words, for class loader
     * {@code cl} and every interface {@code i}, the following
     * expression must be true:
     * <pre>
     *     Class.forName(i.getName(), false, cl) == i
     * </pre>
     *
     * <li>All non-public interfaces must be in the same package;
     * otherwise, it would not be possible for the proxy class to
     * implement all of the interfaces, regardless of what package it is
     * defined in.
     *
     * <li>For any set of member methods of the specified interfaces
     * that have the same signature:
     * <ul>
     * <li>If the return type of any of the methods is a primitive
     * type or void, then all of the methods must have that same
     * return type.
     * <li>Otherwise, one of the methods must have a return type that
     * is assignable to all of the return types of the rest of the
     * methods.
     * </ul>
     *
     * <li>The resulting proxy class must not exceed any limits imposed
     * on classes by the virtual machine.  For example, the VM may limit
     * the number of interfaces that a class may implement to 65535; in
     * that case, the size of the {@code interfaces} array must not
     * exceed 65535.
     * </ul>
     *
     * <p>If any of these restrictions are violated,
     * {@code Proxy.getProxyClass} will throw an
     * {@code IllegalArgumentException}.  If the {@code interfaces}
     * array argument or any of its elements are {@code null}, a
     * {@code NullPointerException} will be thrown.
     *
     * <p>Note that the order of the specified proxy interfaces is
     * significant: two requests for a proxy class with the same combination
     * of interfaces but in a different order will result in two distinct
     * proxy classes.
     *
     * @param   loader the class loader to define the proxy class
     * @param   interfaces the list of interfaces for the proxy class
     *          to implement
     * @return  a proxy class that is defined in the specified class loader
     *          and that implements the specified interfaces
     * @throws  IllegalArgumentException if any of the restrictions on the
     *          parameters that may be passed to {@code getProxyClass}
     *          are violated
     * @throws  NullPointerException if the {@code interfaces} array
     *          argument or any of its elements are {@code null}
     */
    public static Class<?> getProxyClass(ClassLoader loader,
                                         Class<?>... interfaces)
        throws IllegalArgumentException
    {
        if (interfaces.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded");
        }

        /* collect interface names to use as key for proxy class cache */
        String[] interfaceNames = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = interfaces[i].getName();
        }

        /*
         * Using string representations of the proxy interfaces as
         * keys in the proxy class cache (instead of their Class
         * objects) is sufficient because we require the proxy
         * interfaces to be resolvable by name through the supplied
         * class loader, and it has the advantage that using a string
         * representation of a class makes for an implicit weak
         * reference to the class.
         */
        Object key = new Key(interfaceNames);

        /*
         * Get the proxy class cache for the class loader.
         */
        ConcurrentMap<Object, Supplier<Class<?>>> cache = getProxyClassCache(loader);

        /*
         * Look up the list of interfaces in the proxy class cache using
         * the key. This will get us a Supplier for the proxy class.
         */
        Supplier<Class<?>> supplier = cache.get(key);

        /*
         * Evaluate the supplier if already cached
         */
        if (supplier != null) {
            return supplier.get();
        }

        /*
         * Verify parameters only when fast-path fails...
         */
        Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);

        for (int i = 0; i < interfaces.length; i++) {
            /*
             * Verify that the class loader resolves the name of this
             * interface to the same Class object.
             */
            String interfaceName = interfaces[i].getName();
            Class<?> interfaceClass = null;
            try {
                interfaceClass = Class.forName(interfaceName, false, loader);
            } catch (ClassNotFoundException e) {
            }
            if (interfaceClass != interfaces[i]) {
                throw new IllegalArgumentException(
                    interfaces[i] + " is not visible from class loader");
            }

            /*
             * Verify that the Class object actually represents an
             * interface.
             */
            if (!interfaceClass.isInterface()) {
                throw new IllegalArgumentException(
                    interfaceClass.getName() + " is not an interface");
            }

            /*
             * Verify that this interface is not a duplicate.
             */
            if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
                throw new IllegalArgumentException(
                    "repeated interface: " + interfaceClass.getName());
            }
        }

        /*
         * Construct ProxyClassFactory and put it atomically into the cache.
         */
        supplier = new ProxyClassFactory(loader, interfaces, cache, key);
        Supplier<Class<?>> oldSupplier = cache.putIfAbsent(key, supplier);
        if (oldSupplier != null)
            supplier = oldSupplier;

        /*
         * Evaluate the supplier.
         */
        return supplier.get();
    }

    /**
     * A key composed of an array of interned strings
     */
    private static final class Key {
        private final String[] strings;

        Key(String[] strings) {
            this.strings = strings;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (String s : strings) hash ^= System.identityHashCode(s);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != Key.class) return false;
            String[] otherStrings = ((Key) obj).strings;
            if (strings.length != otherStrings.length) return false;
            for (int i = 0; i < strings.length; i++)
                if (strings[i] != otherStrings[i]) return false;
            return true;
        }
    }

    /**
     * A Supplier that generates and defines the proxy class.
     * It replaces itself in the cache with the constant supplier upon success or
     * removes itself from the cache upon failure.
     */
    private static final class ProxyClassFactory implements Supplier<Class<?>> {

        /** prefix for all proxy class names */
        private static final String proxyClassNamePrefix = "$Proxy";

        /** next number to use for generation of unique proxy class names */
        private static final AtomicLong nextUniqueNumber = new AtomicLong();

        private final ClassLoader loader;
        private final Class<?>[] interfaces;
        private final ConcurrentMap<Object, Supplier<Class<?>>> cache;
        private final Object key;

        ProxyClassFactory(ClassLoader loader, Class<?>[] interfaces,
                          ConcurrentMap<Object, Supplier<Class<?>>> cache,
                          Object key) {
            this.loader = loader;
            this.interfaces = interfaces;
            this.cache = cache;
            this.key = key;
        }

        /*
         * Synchronized get method so any concurrent requests for
         * the same proxy class will be serialized.
         */
        @Override
        public synchronized Class<?> get() {

            /*
             * Re-check that it's still us...
             */
            Supplier<Class<?>> supplier = cache.get(key);
            if (supplier != null && supplier != this) {
                // already replaced with constant supplier
                return supplier.get();
            }
            // else still us or removed because of failure...

            boolean success = false;
            try {
                String proxyPkg = null;     // package to define proxy class in

                /*
                 * Record the package of a non-public proxy interface so that the
                 * proxy class will be defined in the same package.  Verify that
                 * all non-public proxy interfaces are in the same package.
                 */
                for (int i = 0; i < interfaces.length; i++) {
                    int flags = interfaces[i].getModifiers();
                    if (!Modifier.isPublic(flags)) {
                        String name = interfaces[i].getName();
                        int n = name.lastIndexOf('.');
                        String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                        if (proxyPkg == null) {
                            proxyPkg = pkg;
                        } else if (!pkg.equals(proxyPkg)) {
                            throw new IllegalArgumentException(
                                "non-public interfaces from different packages");
                        }
                    }
                }

                if (proxyPkg == null) {     // if no non-public proxy interfaces,
                    proxyPkg = "";          // use the unnamed package
                }

                /*
                 * Choose a name for the proxy class to generate.
                 */
                long num = nextUniqueNumber.getAndIncrement();
                String proxyName = proxyPkg + proxyClassNamePrefix + num;
                /*
                 * Verify that the class loader hasn't already
                 * defined a class with the chosen name. TODO?
                 */

                /*
                 * Generate the specified proxy class.
                 */
                byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
                    proxyName, interfaces);
                Class<?> proxyClass;
                try {
                    proxyClass = defineClass0(loader, proxyName,
                        proxyClassFile, 0, proxyClassFile.length);
                } catch (ClassFormatError e) {
                    /*
                     * A ClassFormatError here means that (barring bugs in the
                     * proxy class generation code) there was some other
                     * invalid aspect of the arguments supplied to the proxy
                     * class creation (such as virtual machine limitations
                     * exceeded).
                     */
                    throw new IllegalArgumentException(e.toString());
                }

                /*
                 * Mark the status of class to be a proxy class.
                 */
                proxyClassInConstruction.set(Boolean.TRUE);
                try {
                    if (isProxyClass.get(proxyClass) != Boolean.TRUE)
                        throw new AssertionError("Internal inconsistency");
                }
                finally {
                    proxyClassInConstruction.remove();
                }

                /*
                 * Replace us with a constant supplier upon success.
                 */
                cache.replace(key, this, new ProxyClassHolder(proxyClass));
                success = true;

                return proxyClass;
            }
            finally {
                /*
                 * Remove us from the supplier cache upon failure.
                 */
                if (!success) {
                    cache.remove(key, this);
                }
            }
        }
    }

    /**
     * A supplier that holds already constructed proxy class.
     * For fast-path retrieval.
     */
    static final class ProxyClassHolder implements Supplier<Class<?>> {
        private final Class<?> proxyClass;

        ProxyClassHolder(Class<?> proxyClass) {
            this.proxyClass = proxyClass;
        }

        @Override
        public Class<?> get() {
            return proxyClass;
        }
    }

    /**
     * Returns an instance of a proxy class for the specified interfaces
     * that dispatches method invocations to the specified invocation
     * handler.  This method is equivalent to:
     * <pre>
     *     Proxy.getProxyClass(loader, interfaces).
     *         getConstructor(new Class[] { InvocationHandler.class }).
     *         newInstance(new Object[] { handler });
     * </pre>
     *
     * <p>{@code Proxy.newProxyInstance} throws
     * {@code IllegalArgumentException} for the same reasons that
     * {@code Proxy.getProxyClass} does.
     *
     * @param   loader the class loader to define the proxy class
     * @param   interfaces the list of interfaces for the proxy class
     *          to implement
     * @param   h the invocation handler to dispatch method invocations to
     * @return  a proxy instance with the specified invocation handler of a
     *          proxy class that is defined by the specified class loader
     *          and that implements the specified interfaces
     * @throws  IllegalArgumentException if any of the restrictions on the
     *          parameters that may be passed to {@code getProxyClass}
     *          are violated
     * @throws  NullPointerException if the {@code interfaces} array
     *          argument or any of its elements are {@code null}, or
     *          if the invocation handler, {@code h}, is
     *          {@code null}
     */
    public static Object newProxyInstance(ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h)
        throws IllegalArgumentException
    {
        if (h == null) {
            throw new NullPointerException();
        }

        /*
         * Look up or generate the designated proxy class.
         */
        Class<?> cl = getProxyClass(loader, interfaces);

        /*
         * Invoke its constructor with the designated invocation handler.
         */
        try {
            Constructor<?> cons = cl.getConstructor(constructorParams);
            return cons.newInstance(new Object[] { h });
        } catch (NoSuchMethodException |
                 IllegalAccessException |
                 InstantiationException |
                 InvocationTargetException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    /**
     * Returns true if and only if the specified class was dynamically
     * generated to be a proxy class using the {@code getProxyClass}
     * method or the {@code newProxyInstance} method.
     *
     * <p>The reliability of this method is important for the ability
     * to use it to make security decisions, so its implementation should
     * not just test if the class in question extends {@code Proxy}.
     *
     * @param   cl the class to test
     * @return  {@code true} if the class is a proxy class and
     *          {@code false} otherwise
     * @throws  NullPointerException if {@code cl} is {@code null}
     */
    public static boolean isProxyClass(Class<?> cl) {
        return Proxy.class.isAssignableFrom(cl) && isProxyClass.get(cl);
    }

    /**
     * Returns the invocation handler for the specified proxy instance.
     *
     * @param   proxy the proxy instance to return the invocation handler for
     * @return  the invocation handler for the proxy instance
     * @throws  IllegalArgumentException if the argument is not a
     *          proxy instance
     */
    public static InvocationHandler getInvocationHandler(Object proxy)
        throws IllegalArgumentException
    {
        /*
         * Verify that the object is actually a proxy instance.
         */
        if (!isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }

        Proxy p = (Proxy) proxy;
        return p.h;
    }

    private static native Class<?> defineClass0(ClassLoader loader, String name,
                                                byte[] b, int off, int len);

    //
    // Unsafe machinery for accessing cache in j.l.ClassLoader

    private static final Unsafe unsafe;
    private static final long proxyClassCacheOffset;

    static {
        unsafe = Unsafe.getUnsafe();
        try {
            proxyClassCacheOffset = unsafe.objectFieldOffset(
                ClassLoader.class.getDeclaredField("proxyClassCache")
            );
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static ConcurrentMap<Object, Supplier<Class<?>>> getProxyClassCache(ClassLoader cl) {
        if (cl == null) return bootstrapCLProxyClassCache;

        Object cache = unsafe.getObjectVolatile(cl, proxyClassCacheOffset);

        if (cache == null) {
            // we shall set this only once...
            if (!unsafe.compareAndSwapObject(cl, proxyClassCacheOffset, null, cache = new ConcurrentHashMap<>()))
                cache = unsafe.getObjectVolatile(cl, proxyClassCacheOffset);
        }

        return (ConcurrentMap<Object, Supplier<Class<?>>>) cache;
    }
}
