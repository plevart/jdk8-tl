/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import si.pele.microbench.TestRunner;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author peter
 */
public class BigIntegerToStringPerf extends TestRunner {

    public static class Big1000ToString10Test extends Test {
        static final BigInteger big;
        static {
            Random rnd = new Random(0L);
            StringBuilder sb = new StringBuilder();
            sb.append(rnd.nextInt(9) + 1);
            for (int i = 0; i < 1000; i++)
                sb.append(rnd.nextInt(10));
            big = new BigInteger(sb.toString());
        }

        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration())
                devNull1.yield(big.toString(10));
        }
    }

    public static void main(String[] args) throws Exception {
        doTest(Big1000ToString10Test.class, 5000L, 1, 4, 1);
    }
}
