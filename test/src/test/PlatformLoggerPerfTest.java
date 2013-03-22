package test;

import java.util.logging.Level;
import java.util.logging.Logger;

import sun.util.logging.PlatformLogger;

/**
 * PlatformLogger patch Test (performance / memory overhead)
 * @author bourgesl
 */
public class PlatformLoggerPerfTest {

    public static void main(String[] args) {

        final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11");

        log.setLevel(PlatformLogger.INFO);

        /*
         * Note: -XX:-UseTLAB because Thread local allocator is efficient (fast enough) to deal with big Integer allocations
         * 
         * 1/ JVM options during tests:
         * -Xms8m -Xmx8m -XX:-UseTLAB -XX:+PrintTLAB
         * 
         * JDK7_13 results:
         mars 21, 2013 11:15:07 AM test.PlatformLoggerTest main
         INFO: PlatformLoggerTest: start on JVM1.7.0_13 [Java HotSpot(TM) 64-Bit Server VM 23.7-b01]
         * 
         INFO: testPerf[100000 iterations]: duration = 61.536460999999996 ms.
         INFO: PlatformLoggerTest: starting 100000000 iterations ...
         INFO: testPerf[100000000 iterations]: duration = 10485.07581 ms.
         INFO: testPerf[100000000 iterations]: duration = 10639.329926 ms.
         INFO: testPerf[100000000 iterations]: duration = 10903.235198 ms.
         INFO: testPerf[100000000 iterations]: duration = 10728.399372 ms.
         INFO: testPerf[100000000 iterations]: duration = 10643.329983 ms.
         INFO: testPerf[100000000 iterations]: duration = 10720.43687 ms.
         INFO: testPerf[100000000 iterations]: duration = 10864.371595999999 ms.
         INFO: testPerf[100000000 iterations]: duration = 10713.845459 ms.
         INFO: testPerf[100000000 iterations]: duration = 10458.257711 ms.
         INFO: testPerf[100000000 iterations]: duration = 10606.267606 ms.
         * 
         * OpenJDK8 (+patch):
         mars 21, 2013 11:19:03 AM test.PlatformLoggerTest main
         Infos: PlatformLoggerTest: start on JVM1.8.0-internal [OpenJDK 64-Bit Server VM 25.0-b22]
         * 
         Infos: testPerf[100000 iterations]: duration = 21.897412 ms.
         Infos: PlatformLoggerTest: starting 100000000 iterations ...
         Infos: testPerf[100000000 iterations]: duration = 1075.118755 ms.
         Infos: testPerf[100000000 iterations]: duration = 1056.1246059999999 ms.
         Infos: testPerf[100000000 iterations]: duration = 1060.9008629999998 ms.
         Infos: testPerf[100000000 iterations]: duration = 1069.021469 ms.
         Infos: testPerf[100000000 iterations]: duration = 1038.554424 ms.
         Infos: testPerf[100000000 iterations]: duration = 1019.426802 ms.
         Infos: testPerf[100000000 iterations]: duration = 1022.722049 ms.
         Infos: testPerf[100000000 iterations]: duration = 1037.787366 ms.
         Infos: testPerf[100000000 iterations]: duration = 1029.124353 ms.
         Infos: testPerf[100000000 iterations]: duration = 1026.0543639999999 ms.
         * 
         * 2/ JVM options during tests:
         * -Xms8m -Xmx8m -XX:+UseTLAB
         * 
         * JDK7_13 results:
         * mars 21, 2013 12:58:37 PM test.PlatformLoggerTest main
         INFO: PlatformLoggerTest: start on JVM1.7.0_13 [Java HotSpot(TM) 64-Bit Server VM 23.7-b01]
         * 
         INFO: testPerf[100000 iterations]: duration = 55.329637 ms.
         INFO: PlatformLoggerTest: starting 100000000 iterations ...
         INFO: testPerf[100000000 iterations]: duration = 2553.872667 ms.
         INFO: testPerf[100000000 iterations]: duration = 2327.072791 ms.
         INFO: testPerf[100000000 iterations]: duration = 2324.000677 ms.
         INFO: testPerf[100000000 iterations]: duration = 2326.0859929999997 ms.
         INFO: testPerf[100000000 iterations]: duration = 2325.34332 ms.
         INFO: testPerf[100000000 iterations]: duration = 2322.579729 ms.
         INFO: testPerf[100000000 iterations]: duration = 2322.170814 ms.
         INFO: testPerf[100000000 iterations]: duration = 2324.055535 ms.
         INFO: testPerf[100000000 iterations]: duration = 2432.6784829999997 ms.
         INFO: testPerf[100000000 iterations]: duration = 2335.47692 ms.
         * 
         * OpenJDK8 (+patch):
         mars 21, 2013 1:00:30 PM test.PlatformLoggerTest main
         Infos: PlatformLoggerTest: start on JVM1.8.0-internal [OpenJDK 64-Bit Server VM 25.0-b22]
         * 
         Infos: testPerf[100000 iterations]: duration = 28.996046 ms.
         Infos: PlatformLoggerTest: starting 100000000 iterations ...
         Infos: testPerf[100000000 iterations]: duration = 1015.773196 ms.
         Infos: testPerf[100000000 iterations]: duration = 1000.8395019999999 ms.
         Infos: testPerf[100000000 iterations]: duration = 1000.3945329999999 ms.
         Infos: testPerf[100000000 iterations]: duration = 1002.113027 ms.
         Infos: testPerf[100000000 iterations]: duration = 1005.377165 ms.
         Infos: testPerf[100000000 iterations]: duration = 1002.030398 ms.
         Infos: testPerf[100000000 iterations]: duration = 1001.7021209999999 ms.
         Infos: testPerf[100000000 iterations]: duration = 1001.343045 ms.
         Infos: testPerf[100000000 iterations]: duration = 1002.1781659999999 ms.
         Infos: testPerf[100000000 iterations]: duration = 1086.250887 ms.
         * 
         * That's all folks!
         */

        /** logger - enable java.util.logging to enable PlatformLogger using JUL */
        final Logger logger = Logger.getLogger(PlatformLoggerPerfTest.class.getName());

        logger.info("PlatformLoggerTest: start on JVM" + System.getProperty("java.version") + " [" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + "]");

        final boolean testEnabledLogs = false;

        logger.info("PlatformLogger: enabled = " + log.isEnabled());
        logger.info("PlatformLogger: level = " + log.getLevel());

        /*
         for (Enumeration<String> e = LogManager.getLogManager().getLoggerNames(); e.hasMoreElements();) {
         logger.info("PlatformLoggerTest: logger[" + e.nextElement() + "]");
         }
         */

        // Cleanup before test:
        cleanup();


        int nLog = 0;

        final int WARMUP = 100 * 1000;

        long start = System.nanoTime();

        for (int i = 0; i < WARMUP; i++) {

            if (log.isLoggable(PlatformLogger.FINEST)) {
                log.finest("test PlatformLogger.FINEST");
                nLog++; // ensure hotspot do not skip isLoggable()
            } else {
                nLog--;
            }

            if (log.isLoggable(PlatformLogger.FINE)) {
                log.fine("test PlatformLogger.FINE");
                nLog++; // ensure hotspot do not skip isLoggable()
            } else {
                nLog--;
            }

            if (log.isLoggable(PlatformLogger.FINER)) {
                log.finer("test PlatformLogger.FINER");
                nLog++; // ensure hotspot do not skip isLoggable()
            } else {
                nLog--;
            }

            if (log.isLoggable(PlatformLogger.CONFIG)) {
                log.config("test PlatformLogger.CONFIG");
                nLog++; // ensure hotspot do not skip isLoggable()
            } else {
                nLog--;
            }

            if (testEnabledLogs) {
                if (log.isLoggable(PlatformLogger.INFO)) {
                    log.info("test PlatformLogger.INFO");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.WARNING)) {
                    log.warning("test PlatformLogger.WARNING");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.SEVERE)) {
                    log.severe("test PlatformLogger.SEVERE");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.OFF)) {
                    log.severe("test PlatformLogger.OFF");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }
            }
        }

        log.info("testPerf[" + WARMUP + " iterations]: duration = " + (1e-6d * (System.nanoTime() - start)) + " ms.");
        log.info("testPerf: nLog = " + nLog);
        cleanup();


        final int PASS = 10;
        final int N = 100 * 1000 * 1000;
        logger.info("PlatformLoggerTest: starting " + N + " iterations ...");

        for (int j = 0; j < PASS; j++) {
            nLog = 0;
            start = System.nanoTime();

            for (int i = 0; i < N; i++) {

                if (log.isLoggable(PlatformLogger.FINEST)) {
                    log.finest("test PlatformLogger.FINEST");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.FINE)) {
                    log.fine("test PlatformLogger.FINE");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.FINER)) {
                    log.finer("test PlatformLogger.FINER");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (log.isLoggable(PlatformLogger.CONFIG)) {
                    log.config("test PlatformLogger.CONFIG");
                    nLog++; // ensure hotspot do not skip isLoggable()
                } else {
                    nLog--;
                }

                if (testEnabledLogs) {
                    if (log.isLoggable(PlatformLogger.INFO)) {
                        log.info("test PlatformLogger.INFO");
                        nLog++; // ensure hotspot do not skip isLoggable()
                    } else {
                        nLog--;
                    }

                    if (log.isLoggable(PlatformLogger.WARNING)) {
                        log.warning("test PlatformLogger.WARNING");
                        nLog++; // ensure hotspot do not skip isLoggable()
                    } else {
                        nLog--;
                    }

                    if (log.isLoggable(PlatformLogger.SEVERE)) {
                        log.severe("test PlatformLogger.SEVERE");
                        nLog++; // ensure hotspot do not skip isLoggable()
                    } else {
                        nLog--;
                    }

                    if (log.isLoggable(PlatformLogger.OFF)) {
                        log.severe("test PlatformLogger.OFF");
                        nLog++; // ensure hotspot do not skip isLoggable()
                    } else {
                        nLog--;
                    }
                }
            }

            log.info("testPerf[" + N + " iterations]: duration = " + (1e-6d * (System.nanoTime() - start)) + " ms.");
            log.info("testPerf: nLog = " + nLog);
            cleanup();
        }

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException ie) {
            logger.log(Level.SEVERE, "Interrupted", ie);
        }

        logger.info("PlatformLoggerTest: exit.");
    }

    /**
     * Cleanup (GC + pause)
     */
    private static void cleanup() {
        // Perform GC:
        System.gc();

        // pause:
        try {
            Thread.sleep(100l);
        } catch (InterruptedException ie) {
        }
    }
}