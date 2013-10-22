/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * @author peter
 */
public class InheritedRepeatingAnnotationsTest
{
    static void dump(Class<?> clazz) {
        System.out.println(clazz + ": " + Arrays.toString(clazz.getAnnotationsByType(Ann.class)));
    }

    public static void main(String[] args)
    {
        dump(A2.class);
        dump(B2.class);
        dump(C2.class);
        dump(D2.class);
        System.out.println();
        dump(A3.class);
        dump(B3.class);
        dump(C3.class);
        dump(D3.class);
    }
}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AnnCont.class)
@interface Ann {
    int value();
}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AnnCont {
    Ann[] value();
}


@Ann(10)
class A1 {}

@Ann(20)
class A2 extends A1 {}

class A3 extends A2 {}


@Ann(10) @Ann(11)
class B1 {}

@Ann(20)
class B2 extends B1 {}

class B3 extends B2 {}


@Ann(10)
class C1 {}

@Ann(20) @Ann(21)
class C2 extends C1 {}

class C3 extends C2 {}


@Ann(10) @Ann(11)
class D1 {}

@Ann(20) @Ann(21)
class D2 extends D1 {}

class D3 extends D2 {}
