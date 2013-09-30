/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * @author peter
 */
public class RepeatingAnns {

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(AnnCont.class)
    @Inherited
    @interface Ann {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface AnnCont {
        Ann[] value();
    }

    @Ann(0) @Ann(1)
    static class A {}

    @Ann(2) @Ann(3)
    static class B extends A {}

    @Ann(0)
    static class F {}

    @Ann(2)
    static class D extends F {}


    public static void main(String[] args) {

        System.out.println("A.declaredAnnotations: " + Arrays.toString(A.class.getDeclaredAnnotations()));
        System.out.println("F.declaredAnnotations: " + Arrays.toString(F.class.getDeclaredAnnotations()));

        Ann[] bResult = B.class.getAnnotationsByType(Ann.class);
        System.out.println("B @Ann (s): " + Arrays.toString(bResult));

        Ann[] dResult = D.class.getAnnotationsByType(Ann.class);
        System.out.println("D @Ann (s): " + Arrays.toString(dResult));
    }
}
