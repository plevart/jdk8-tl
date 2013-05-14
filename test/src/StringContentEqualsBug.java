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

/**
 * @test
 * @bug 8014477
 * @summary test String.contentEquals(StringBuffer)
 */
public class StringContentEqualsBug {

    static class Task extends Thread {
        volatile StringBuffer sb;
        volatile Exception exception;

        Task(StringBuffer sb) {
            this.sb = sb;
        }

        @Override
        public void run() {
            try {
                StringBuffer sb;
                while ((sb = this.sb) != null) {
                    "QQ".contentEquals(sb);
                    sb.setLength(0);
                    sb.trimToSize();
                    sb.append("AA");
                }
            }
            catch (Exception e) {
                exception = e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        StringBuffer sb = new StringBuffer();
        Task[] tasks = new Task[3];
        for (int i = 0; i < tasks.length; i++) {
            (tasks[i] = new Task(sb)).start();
        }
        try
        {
            // wait at most 5 seconds for any of the threads to throw exception
            for (int i = 0; i < 20; i++) {
                for (Task task : tasks) {
                    if (task.exception != null) {
                        throw task.exception;
                    }
                }
                Thread.sleep(250L);
            }
        }
        finally {
            for (Task task : tasks) {
                task.sb = null;
                task.join();
            }
        }
    }
}
