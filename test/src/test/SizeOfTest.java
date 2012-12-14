package test;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 536
 1016
 1064
 1272
 1472
 3232


 */
public class SizeOfTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Ann {}

    class Cls {
        int f;
        void m() {}
    }

    public static void main(String[] args) {

        new SizeOf(SizeOf.Visitor.STDOUT).deepSizeOf(Object.class);

        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);

        System.out.println(sizeOf.deepSizeOf(Cls.class));

        Cls.class.getDeclaredFields();
        System.out.println(sizeOf.deepSizeOf(Cls.class));

        Cls.class.getFields();
        System.out.println(sizeOf.deepSizeOf(Cls.class));

        Cls.class.getDeclaredConstructors();
        System.out.println(sizeOf.deepSizeOf(Cls.class));

        Cls.class.getDeclaredMethods();
        System.out.println(sizeOf.deepSizeOf(Cls.class));

        Cls.class.getMethods();
        System.out.println(sizeOf.deepSizeOf(Cls.class));
    }
}
