/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author peter
 */
public class AnnTypeBench extends TestRunner
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Ann {}

    @Ann
    static class C {}

    static final Ann ann = C.class.getDeclaredAnnotation(Ann.class);

    public static class isInstance extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5)
        {
            while (loop.nextIteration()) {
                devNull1.yield(Ann.class.isInstance(ann));
            }
        }
    }

    public static class annTypeEq extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5)
        {
            while (loop.nextIteration()) {
                devNull1.yield(ann.annotationType() == Ann.class);
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        doTest(isInstance.class, 2000L, 1, 8, 1);
        doTest(annTypeEq.class, 2000L, 1, 8, 1);
    }
}
