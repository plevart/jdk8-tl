package test;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 */
public class TestRunner {

    public static abstract class Test extends Thread {
        private static boolean
            p01, p02, p03, p04, p05, p06, p07, p08, p09, p0a, p0b, p0c, p0d, p0e, p0f,
            p11, p12, p13, p14, p15, p16, p17, p18, p19, p1a, p1b, p1c, p1d, p1e, p1f,
            p21, p22, p23, p24, p25, p26, p27, p28, p29, p2a, p2b, p2c, p2d, p2e, p2f,
            p31, p32, p33, p34, p35, p36, p37, p38, p39, p3a, p3b, p3c, p3d, p3e, p3f;
        //
        static volatile boolean run;
        //
        private static boolean
            q01, q02, q03, q04, q05, q06, q07, q08, q09, q0a, q0b, q0c, q0d, q0e, q0f,
            q11, q12, q13, q14, q15, q16, q17, q18, q19, q1a, q1b, q1c, q1d, q1e, q1f,
            q21, q22, q23, q24, q25, q26, q27, q28, q29, q2a, q2b, q2c, q2d, q2e, q2f,
            q31, q32, q33, q34, q35, q36, q37, q38, q39, q3a, q3b, q3c, q3d, q3e, q3f;

        private CountDownLatch startLatch, stopLatch;
        long ops, nanos;

        void start(CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.startLatch = startLatch;
            this.stopLatch = stopLatch;
            super.start();
        }

        @Override
        public void run() {
            long ops = 0L;
            startLatch.countDown();
            while (!run) {
                doOp();
            }
            long t0 = System.nanoTime();
            while (run) {
                doOp();
                ops++;
            }
            long t1 = System.nanoTime();
            stopLatch.countDown();
            while (!run) {
                doOp();
            }
            this.ops = ops;
            this.nanos = t1 - t0;
        }

        protected abstract void doOp();

        private long
            r01, r02, r03, r04, r05, r06, r07, r08, r09, r0a, r0b, r0c, r0d, r0e, r0f,
            r11, r12, r13, r14, r15, r16, r17, r18, r19, r1a, r1b, r1c, r1d, r1e, r1f;
        //
        long nulls, nonnulls;
        //
        private long
            s01, s02, s03, s04, s05, s06, s07, s08, s09, s0a, s0b, s0c, s0d, s0e, s0f,
            s11, s12, s13, s14, s15, s16, s17, s18, s19, s1a, s1b, s1c, s1d, s1e, s1f;

        protected final void consume(Object o) {
            if (o == null)
                nulls++;
            else
                nonnulls++;
        }

        protected final void consume(int i) {
            if (i == 0)
                nulls++;
            else
                nonnulls++;
        }

        protected final void consume(boolean b) {
            if (b)
                nonnulls++;
            else
                nulls++;
        }
    }

    public static class Result {
        public final String testName;
        public final int threads;
        public final double nsPerOpAvg, nsPerOpSigma;
        private final double nsPerOps[];

        public Result(String testName, int threads, double nsPerOpAvg, double nsPerOpSigma, double nsPerOps[]) {
            this.testName = testName;
            this.threads = threads;
            this.nsPerOpAvg = nsPerOpAvg;
            this.nsPerOpSigma = nsPerOpSigma;
            this.nsPerOps = nsPerOps;
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean dumpIndividualThreads) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%30s: %3d threads, Tavg = %,9.2f ns/op (σ = %,6.2f ns/op)", testName, threads, nsPerOpAvg, nsPerOpSigma));
            if (dumpIndividualThreads) {
                sb.append(" [");
                for (int i = 0; i < nsPerOps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(String.format("%,9.2f ns/op", nsPerOps[i]));
                }
                sb.append("]");
            }
            return sb.toString();
        }
    }

    protected static Result runTest(Supplier<? extends Test> testFactory, long runDurationMillis, int threads) throws InterruptedException {
        Test[] tests = new Test[threads];
        for (int i = 0; i < threads; i++)
            tests[i] = testFactory.get();
        CountDownLatch startLatch = new CountDownLatch(threads);
        CountDownLatch stopLatch = new CountDownLatch(threads);
        Test.run = false;
        for (Test test : tests)
            test.start(startLatch, stopLatch);
        startLatch.await();
        Thread.sleep(100L); // pre-run overlap
        Test.run = true;
        Thread.sleep(runDurationMillis);
        Test.run = false;
        stopLatch.await();
        Thread.sleep(100L); // post-run overlap
        Test.run = true;
        long opsSum = 0L;
        long nanosSum = 0L;
        for (Test test : tests) {
            test.join();
            if (test.nulls == 0L && test.nonnulls == 0L)
                throw new IllegalStateException("No black hole usage");
            opsSum += test.ops;
            nanosSum += test.nanos;
        }
        Test.run = false;
        double nsPerOpAvg = (double) nanosSum / (double) opsSum;
        double nsPerOpVar = 0d;
        double nsPerOps[] = new double[threads];
        for (int i = 0; i < threads; i++) {
            Test test = tests[i];
            double nsPerOp = (double) test.nanos / (double) test.ops;
            nsPerOps[i] = nsPerOp;
            double nsPerOpDiff = nsPerOpAvg - nsPerOp;
            nsPerOpVar += nsPerOpDiff * nsPerOpDiff;
        }
        nsPerOpVar /= (double) threads;
        return new Result(tests[0].getClass().getSimpleName(), threads, nsPerOpAvg, Math.sqrt(nsPerOpVar), nsPerOps);
    }

    protected static Result runTest(final Class<? extends Test> testClass, long runDurationMillis, int threads) throws InterruptedException {
        return runTest(
            new Supplier<Test>() {
                @Override
                public Test get() {
                    try {
                        return testClass.newInstance();
                    }
                    catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            },
            runDurationMillis,
            threads
        );
    }
}