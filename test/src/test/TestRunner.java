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

        long ops;
        private CountDownLatch latch;

        void start(CountDownLatch latch) {
            this.latch = latch;
            super.start();
        }

        @Override
        public void run() {
            long ops = 0L;
            latch.countDown();
            while (!run) {}
            while (run) {
                doOp();
                ops++;
            }
            this.ops = ops;
        }

        protected abstract void doOp();

        private Object
            r01, r02, r03, r04, r05, r06, r07, r08, r09, r0a, r0b, r0c, r0d, r0e, r0f,
            r11, r12, r13, r14, r15, r16, r17, r18, r19, r1a, r1b, r1c, r1d, r1e, r1f;
        //
        private volatile Object blackHole;
        //
        private Object
            s01, s02, s03, s04, s05, s06, s07, s08, s09, s0a, s0b, s0c, s0d, s0e, s0f,
            s11, s12, s13, s14, s15, s16, s17, s18, s19, s1a, s1b, s1c, s1d, s1e, s1f;

        protected final void consume(Object o) {
            blackHole = o;
        }
    }

    public static class Result {
        public final String testName;
        public final int threads;
        public final long runTimeNanos;
        public final double nsPerOp, nsPerOpSigma;

        public Result(String testName, int threads, long runTimeNanos, double nsPerOp, double nsPerOpSigma) {
            this.testName = testName;
            this.threads = threads;
            this.runTimeNanos = runTimeNanos;
            this.nsPerOp = nsPerOp;
            this.nsPerOpSigma = nsPerOpSigma;
        }

        @Override
        public String toString() {
            return String.format("%30s: %3d threads, %,11.4f +- %,8.4f ns/op", testName, threads, nsPerOp, nsPerOpSigma);
        }
    }

    protected static Result runTest(Supplier<? extends Test> testFactory, long runDurationMillis, int threads) throws InterruptedException {
        Test[] tests = new Test[threads];
        for (int i = 0; i < threads; i++)
            tests[i] = testFactory.get();
        CountDownLatch latch = new CountDownLatch(threads);
        Test.run = false;
        for (Test test : tests)
            test.start(latch);
        latch.await();
        long t0 = System.nanoTime();
        Test.run = true;
        Thread.sleep(runDurationMillis);
        Test.run = false;
        double nanos = (double) (System.nanoTime() - t0);
        long opsSum = 0L;
        for (Test test : tests) {
            test.join();
            if (test.blackHole == null)
                throw new IllegalStateException("No black hole usage");
            opsSum += test.ops;
        }
        double nsPerOpAvg = nanos * (double) threads / (double) opsSum;
        double nsPerOpVar = 0d;
        for (Test test : tests) {
            double nsPerOp = nanos / (double) test.ops;
            double nsPerOpDiff = nsPerOpAvg - nsPerOp;
            nsPerOpVar += nsPerOpDiff * nsPerOpDiff;
        }
        nsPerOpAvg /= (double) threads;
        return new Result(tests[0].getClass().getSimpleName(), threads, (long) nanos, nsPerOpAvg, Math.sqrt(nsPerOpVar));
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