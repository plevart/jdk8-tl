/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 6857566
 * @summary DirectByteBuffer garbage creation can outpace reclamation
 *
 * @run main/othervm -XX:MaxDirectMemorySize=128m DirectBufferAllocTest
 */

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

public class DirectBufferAllocTest {
    // defaults
    static final int RUN_TIME_SECONDS = 10;
    static final int MIN_THREADS = 4;
    static final int MAX_THREADS = 64;
    static final int CAPACITY = 1024 * 1024; // bytes

    public static void main(String[] args) throws Exception {
        int runTimeSeconds = RUN_TIME_SECONDS;
        int threads = Math.max(
            Math.min(
                Runtime.getRuntime().availableProcessors() * 2,
                MAX_THREADS
            ),
            MIN_THREADS
        );
        int capacity = CAPACITY;
        int printTimeBatchSize = 0;

        // override with command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-r":
                    runTimeSeconds = Integer.parseInt(args[++i]);
                    break;
                case "-t":
                    threads = Integer.parseInt(args[++i]);
                    break;
                case "-c":
                    capacity = Integer.parseInt(args[++i]);
                    break;
                case "-p":
                    printTimeBatchSize = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println(
                        "Usage: java" +
                        " [-XX:MaxDirectMemorySize=XXXm]" +
                        " DirectBufferAllocTest" +
                        " [-r run-time-seconds]" +
                        " [-t threads]" +
                        " [-c direct-buffer-capacity]" +
                        " [-p print-time-batch-size]"
                    );
                    System.exit(-1);
            }
        }

        // in case java.nio.Bits is instrumented, we want to access the counters...
        LongAdder[] reserveCounters;
        try {
            Class bitsClass = Class.forName("java.nio.Bits");
            Field reserveCountersField = bitsClass.getDeclaredField("reserveCounters");
            reserveCountersField.setAccessible(true);
            reserveCounters = (LongAdder[]) reserveCountersField.get(null);
        }
        catch (NoSuchFieldException e) {
            reserveCounters = null;
        }

        System.out.println(
            "Allocating direct ByteBuffers with capacity " +
            CAPACITY + " bytes, " +
            "using " + threads + " threads for " + runTimeSeconds + " seconds..."
        );

        for (int i = 0; i < threads; i++) {
            final int ptbs = printTimeBatchSize;
            final int cap = capacity;
            new Thread("thread-" + i) {
                public void run() {
                    int it = 0;
                    try {
                        long t0 = System.nanoTime();
                        for (; ; ) {
                            for (int i = 0; ptbs == 0 || i < ptbs; i++) {
                                ByteBuffer.allocateDirect(cap);
                                it++;
                            }
                            long t1 = System.nanoTime();
                            if (ptbs > 0) {
                                System.out.printf(
                                    "%10s: %5.2f ms/op\n",
                                    getName(),
                                    ((double) (t1 - t0) / (1_000_000d * ptbs))
                                );
                            }
                            t0 = t1;
                        }
                    }
                    catch (OutOfMemoryError t) {
                        System.err.println(
                            Thread.currentThread().getName() +
                            " got an OOM on iteration " + it
                        );
                        t.printStackTrace();
                        System.exit(1);
                    }
                }
            }.start();
        }

        Thread.sleep(1000L * runTimeSeconds);
        System.out.println("No errors after " + runTimeSeconds + " seconds.");
        if (reserveCounters != null) {
            System.out.println("Reserve counters: " + Arrays.toString(reserveCounters));
        }
        System.exit(0);
    }
}
