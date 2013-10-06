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

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class DirectBufferAllocTest {
    static final int MIN_THREADS = 16;
    static final int MAX_THREADS = 128;
    static final int MIN_ALLOC_CAPACITY = 256 * 1024;
    static final int MAX_ALLOC_CAPACITY = 1024 * 1024;
    static final int TIME_MEASURE_BATCH = 10000;
    static final boolean PRINT_ALLOC_TIMES = false;

    public static void main(String[] args) throws InterruptedException {
        // saturate the CPUs!!!
        int threads = Math.max(
            Math.min(
                Runtime.getRuntime().availableProcessors() * 2,
                MAX_THREADS
            ),
            MIN_THREADS
        );

        System.out.println(
            "Allocating direct ByteBuffers with random capacities from " +
            MIN_ALLOC_CAPACITY + " to " + MAX_ALLOC_CAPACITY + " bytes, " +
            "using " + threads + " threads..."
        );

        for (int i = 0; i < threads; i++) {
            new Thread("thread-" + i) {
                public void run() {
                    int it = 0;
                    try {
                        long t0 = System.nanoTime();
                        for (; ; ) {
                            for (int i = 0; i < TIME_MEASURE_BATCH; i++) {
                                ByteBuffer.allocateDirect(
                                    ThreadLocalRandom.current()
                                                     .nextInt(MIN_ALLOC_CAPACITY, MAX_ALLOC_CAPACITY + 1)
                                );
                                it++;
                            }
                            long t1 = System.nanoTime();
                            if (PRINT_ALLOC_TIMES) {
                                System.out.printf(
                                    "%10s: %5.2f ms/op\n",
                                    getName(),
                                    ((double) (t1 - t0) / (1_000_000d * TIME_MEASURE_BATCH))
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

        Thread.sleep(60 * 1000);
        System.out.println("No errors after 60 seconds.");
        System.exit(0);
    }
}
