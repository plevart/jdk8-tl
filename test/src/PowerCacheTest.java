/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;

import java.math.BigInteger;

/**
 * @author peter
 */
public class PowerCacheTest  extends TestRunner {

    public static class TestAleksey extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(BigIntegerPowerCache1.getRadixConversionCache(10, 10));
            }
        }
    }

    public static class TestPeter extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                devNull1.yield(BigIntegerPowerCache2.getRadixConversionCache(10, 10));
            }
        }
    }


    public static void main(String[] args) throws Exception {
        doTest(TestPeter.class, 5000L, 1, 4, 1);
        doTest(TestAleksey.class, 5000L, 1, 4, 1);
    }
}
