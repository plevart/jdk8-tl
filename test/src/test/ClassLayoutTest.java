package test;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

/**
 */
public class ClassLayoutTest {
    private static final Unsafe unsafe;

    static {
        try {
            Field uf = Unsafe.class.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            unsafe = (Unsafe) uf.get(Unsafe.class);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    static void dump(Class<?> clazz, boolean staticFields) {
        Field[] fields = clazz.getDeclaredFields();

        class FieldOffset {
            final Field field;
            final long offset;
            FieldOffset(Field field, long offset) {
                this.field = field;
                this.offset = offset;
            }
        }

        List<FieldOffset> fieldOffsets = new ArrayList<>();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) == staticFields) {
                fieldOffsets.add(new FieldOffset(f, staticFields ? unsafe.staticFieldOffset(f): unsafe.objectFieldOffset(f)));
            }
        }

        Collections.sort(fieldOffsets, new Comparator<FieldOffset>() {
            @Override
            public int compare(FieldOffset fo1, FieldOffset fo2) {
                return Long.compare(fo1.offset, fo2.offset);
            }
        });

        System.out.println();
        System.out.println(clazz.getName() + (staticFields ? " static " : " instance ") + "field offsets:");
        System.out.println();
        System.out.printf("%24s %36s %6s\n", "Field Type", "Field Name", "Offset");
        System.out.printf("%24s %36s %6s\n", "----------", "----------", "------");
        for (FieldOffset fo : fieldOffsets) {
            System.out.printf("%24s %36s %6d\n", fo.field.getType().getSimpleName(), fo.field.getName(), fo.offset);
        }
    }

    public static void main(String[] args) {
        dump(Class.class, false);
        dump(String.class, true);

        Class<?> clazz = Proxy.getProxyClass(ClassLayoutTest.class.getClassLoader(), new Class[0]);
        System.out.println(clazz);
        System.out.println(Arrays.toString(clazz.getInterfaces()));
    }
}
