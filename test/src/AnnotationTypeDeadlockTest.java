/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug
 * @summary Test exposing inconsistent parsing of ex RUNTIME annotations that
 *          were changed and separately compiled to have CLASS retention
 */

import java.lang.annotation.Retention;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class AnnotationTypeDeadlockTest {

    @Retention(RUNTIME)
    @AnnB
    public @interface AnnA {
    }

    @Retention(RUNTIME)
    @AnnA
    public @interface AnnB {
    }

    static class Task extends Thread {
        final AtomicInteger latch;
        final Class<?> clazz;

        Task(AtomicInteger latch, Class<?> clazz) {
            super(clazz.getSimpleName());
            setDaemon(true); // in case it deadlocks
            this.latch = latch;
            this.clazz = clazz;
        }

        @Override
        public void run() {
            latch.incrementAndGet();
            while (latch.get() > 0) ; // spin-wait
            clazz.getDeclaredAnnotations();
        }
    }

    static void dumpState(Task task) {
        Throwable throwable = new Throwable();
        throwable.setStackTrace(task.getStackTrace());
        System.err.println(
            "Task[" + task.getName() + "].state: " +
            task.getState() + " ..."
        );
        throwable.printStackTrace(System.err);
        System.err.println();
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger latch = new AtomicInteger();
        Task taskA = new Task(latch, AnnA.class);
        Task taskB = new Task(latch, AnnB.class);
        taskA.start();
        taskB.start();
        while (latch.get() < 2) ; // spin-wait
        latch.set(0); // trigger coherent start
        taskA.join(1000L);
        taskB.join(1000L);

        if (taskA.isAlive() || taskB.isAlive()) {
            dumpState(taskA);
            dumpState(taskB);
            throw new IllegalStateException("deadlock detected");
        }
    }
}
