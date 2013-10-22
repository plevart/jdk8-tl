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

    static final Class<?>[] classes = { A1.class, A2.class, A1X.class, A2X.class };

    public static void main(String[] args)
    {
        for (Class<?> clazz : classes) {
            System.out.println(clazz + ": " + Arrays.toString(clazz.getDeclaredAnnotations()));
        }
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

@Ann(1)
class A1 {}

@Ann(1) @Ann(2)
class A2 {}

@AnnCont(@Ann(1))
class A1X {}

@AnnCont({@Ann(1), @Ann(2)})
class A2X {}
