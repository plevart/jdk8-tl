/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

import si.pele.microbench.TestRunner;
import sun.misc.PerfCounter;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author peter
 */
public class PerfCounterBenchmark extends TestRunner {

    static final PerfCounter counter = PerfCounter.getFindClasses();
    static final LongAdder adder = new LongAdder();

    public static final class PerfCounter_increment extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                counter.increment();
            }
        }
    }

    public static final class LongAdder_increment extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                adder.increment();
            }
        }
    }

    public static final class PerfCounter_mix extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                long t0 = System.nanoTime();
                PerfCounter.getFindClasses().increment();
                PerfCounter.getFindClassTime().addElapsedTimeFrom(t0);
                PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
                PerfCounter.getParentDelegationTime().addElapsedTimeFrom(t0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        doTest(PerfCounter_mix.class, 5000L, 1, 8, 1);
        doTest(PerfCounter_increment.class, 5000L, 1, 8, 1);
        doTest(LongAdder_increment.class, 5000L, 1, 8, 1);
    }
}
