/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package test;

import si.pele.microbench.TestRunner;
import sun.util.logging.PlatformLogger;

import java.util.logging.LogManager;

/**
 * @author peter
 */
public class PlatformLoggerBenchmark extends TestRunner {

    static final PlatformLogger log = PlatformLogger.getLogger(PlatformLoggerBenchmark.class.getName());

    static {
        log.setLevel(PlatformLogger.SEVERE); // almost OFF
    }

    public static class isLoggableFinest extends Test {
        @Override
        protected void doLoop(Loop loop, DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5) {
            while (loop.nextIteration()) {
                if (log.isLoggable(PlatformLogger.FINEST))
                    log.finest("FINEST LOG");
//                devNull1.yield(log.isLoggable(PlatformLogger.FINEST));
            }
        }
    }


    public static void main(String[] args) throws Exception {

        startTests();

        doTest(isLoggableFinest.class, 3000L, 1, 4, 1, 2, 1);

        // enable java.util.logging
        doAction("java.util.logging enabled", new Runnable() {
            @Override
            public void run() {
                LogManager.getLogManager().getLogger(log.getName());
            }
        });

        doTest(isLoggableFinest.class, 3000L, 1, 4, 1, 0, 3);

        endTests();
    }
}
