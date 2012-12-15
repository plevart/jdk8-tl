package test;


import javax.swing.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> c = clazz; c != null; c = c.getSuperclass())
            classes.add(c);

        System.out.println();
        System.out.println("Deep size of ReflectionData in: " + clazz.getName());
        System.out.println();

        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);

        sizeOf.deepSizeOf(getReflectionData(clazz));

        clazz.getDeclaredConstructors();
        System.out.println("after getDeclaredConstructors: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
        clazz.getDeclaredFields();
        System.out.println("after       getDeclaredFields: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
        clazz.getDeclaredMethods();
        System.out.println("after      getDeclaredMethods: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
        clazz.getConstructors();
        System.out.println("after         getConstructors: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
        clazz.getFields();
        System.out.println("after               getFields: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
        clazz.getMethods();
        System.out.println("after              getMethods: " + sizeOf.deepSizeOf(getReflectionData(clazz)) + " bytes");
    }

    public static void main(String[] args) throws Exception {
        test(HashMap.class);
        test(JTable.class);
    }
}
