package test;


import si.pele.microbench.SizeOf;

import javax.swing.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 */
public class SizeOfTest {
    private static final Field reflectionDataField;

    static {
        try {
            reflectionDataField = Class.class.getDeclaredField("reflectionData");
            reflectionDataField.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static Object getReflectionData(Class<?> clazz) {
        try {
            SoftReference<?> ref = (SoftReference<?>) reflectionDataField.get(clazz);
            return ref == null ? null : ref.get();
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static void test(Class<?> clazz) {

        System.out.println();
        System.out.printf("Deep size of ReflectionData in: %s.class\n", clazz.getName());
        System.out.println();

        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);

        System.out.printf("before              any calls: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getDeclaredConstructors();
        System.out.printf("after getDeclaredConstructors: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getDeclaredFields();
        System.out.printf("after       getDeclaredFields: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getDeclaredMethods();
        System.out.printf("after      getDeclaredMethods: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getConstructors();
        System.out.printf("after         getConstructors: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getFields();
        System.out.printf("after               getFields: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
        clazz.getMethods();
        System.out.printf("after              getMethods: %,7d bytes\n", sizeOf.deepSizeOf(getReflectionData(clazz)));
    }

    public static void main(String[] args) throws Exception {
        test(Object.class);
        test(String.class);
        test(HashMap.class);
        test(JTable.class);
    }
}
