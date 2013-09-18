/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @author peter
 */
public class SerTest implements Serializable {

    public static class Foo implements Serializable {
        final Object[] args;

        public Foo(Object... args) {
            this.args = args;
        }

        Object writeReplace() throws ObjectStreamException {
            return new SerializedFoo(args);
        }

        @Override
        public String toString() {
            return getClass().getName() + "{args=" + Arrays.toString(args) +
                   "}@" + Integer.toHexString(System.identityHashCode(this));
        }
    }

    public static class SerializedFoo implements Serializable {
        final Object[] args;

        public SerializedFoo(Object[] args) {
            this.args = args;
        }

        Object readResolve() throws ObjectStreamException {
            return new Foo(args);
        }

        @Override
        public String toString() {
            return getClass().getName() + "{args=" + Arrays.toString(args) +
                   "}@" + Integer.toHexString(System.identityHashCode(this));
        }
    }

    Foo foo;

    @Override
    public String toString() {
        return getClass().getName() + "{foo=" +
               foo.getClass().getName() + '@' +
               Integer.toHexString(System.identityHashCode(foo)) +
               '}';
    }

    public static void main(String[] args) throws Exception {
        SerTest test = new SerTest();
        test.foo = new Foo(test);

        System.out.println(test.foo);
        Foo foo2 = serialCopy(test.foo);
        System.out.println(foo2);
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    static <T> T serialCopy(T o) throws Exception {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        new ObjectOutputStream(ba).writeObject(o);
        return (T) new ObjectInputStream(new ByteArrayInputStream(ba.toByteArray())).readObject();
    }
}
