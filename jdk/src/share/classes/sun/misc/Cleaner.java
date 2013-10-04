/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.misc;

import java.lang.ref.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * General-purpose phantom-reference-based cleaners.
 *
 * <p> Cleaners are a lightweight and more robust alternative to finalization.
 * They are lightweight because they are not created by the VM and thus do not
 * require a JNI upcall to be created, and because their cleanup code is
 * invoked directly by the reference-handler thread rather than by the
 * finalizer thread.  They are more robust because they use phantom references,
 * the weakest type of reference object, thereby avoiding the nasty ordering
 * problems inherent to finalization.
 *
 * <p> A cleaner tracks a referent object and encapsulates a thunk of arbitrary
 * cleanup code.  Some time after the GC detects that a cleaner's referent has
 * become phantom-reachable, the reference-handler thread will run the cleaner.
 * Cleaners may also be invoked directly; they are thread safe and ensure that
 * they run their thunks at most once.
 *
 * <p> Cleaners are not a replacement for finalization.  They should be used
 * only when the cleanup code is extremely simple and straightforward.
 * Nontrivial cleaners are inadvisable since they risk blocking the
 * reference-handler thread and delaying further cleanup and finalization.
 *
 *
 * @author Mark Reinhold
 */

public class Cleaner
    extends PhantomReference<Object>
{

    // High-priority thread to clean enqueue-ed Cleaners
    //
    private static class CleanerHandler extends Thread {

        CleanerHandler(ThreadGroup g, String name) {
            super(g, name);
        }

        // a lock for pausing normal background cleaning while
        // some other thread is assisting
        final Object assistCleanupLock = new Object();

        public void run() {
            for (;;) {
                try {
                    try {
                        Cleaner c = (Cleaner) cleanersQueue.remove();
                        c.clean();
                    } catch (OutOfMemoryError x) {
                        // if there's heap memory pressure and InterruptedException
                        // can not be allocated, we get OutOfMemoryError instead
                        synchronized (assistCleanupLock) {
                            cleanersQueue.getClass();
                        }
                    }
                } catch (InterruptedException x) {
                    // when interrupted, we park here until assistance
                    // from other threads is finished
                    synchronized (assistCleanupLock) {
                        cleanersQueue.getClass();
                    }
                }
            }
        }
    }

    private static final CleanerHandler handler;
    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent());
        handler = new CleanerHandler(tg, "Cleaner Handler");
        /* If there were a special system-only priority greater than
         * MAX_PRIORITY, it would be used here
         */
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);
        handler.start();
    }

    // Reference queue for cleaners.
    //
    private static final ReferenceQueue<Object> cleanersQueue = new ReferenceQueue<>();

    // Doubly-linked list of live cleaners, which prevents the cleaners
    // themselves from being GC'd before their referents
    //
    static private Cleaner first = null;

    private Cleaner
        next = null,
        prev = null;

    private static synchronized Cleaner add(Cleaner cl) {
        if (first != null) {
            cl.next = first;
            first.prev = cl;
        }
        first = cl;
        return cl;
    }

    private static synchronized boolean remove(Cleaner cl) {

        // If already removed, do nothing
        if (cl.next == cl)
            return false;

        // Update list
        if (first == cl) {
            if (cl.next != null)
                first = cl.next;
            else
                first = cl.prev;
        }
        if (cl.next != null)
            cl.next.prev = cl.prev;
        if (cl.prev != null)
            cl.prev.next = cl.next;

        // Indicate removal by pointing the cleaner to itself
        cl.next = cl;
        cl.prev = cl;
        return true;

    }

    private final Runnable thunk;

    private Cleaner(Object referent, Runnable thunk) {
        super(referent, cleanersQueue);
        this.thunk = thunk;
    }

    /**
     * Creates a new cleaner.
     *
     * @param  thunk
     *         The cleanup code to be run when the cleaner is invoked.  The
     *         cleanup code is run directly from the reference-handler thread,
     *         so it should be as simple and straightforward as possible.
     *
     * @return  The new cleaner
     */
    public static Cleaner create(Object ob, Runnable thunk) {
        if (thunk == null)
            return null;
        return add(new Cleaner(ob, thunk));
    }

    private static final AtomicInteger cleanCount = new AtomicInteger();

    /**
     * Assist with cleaning up the enqueue-ed/pending Cleaners.
     * This method returns the accumulated number of cleans performed so far.
     * If two consecutive invocations of this method return the same value,
     * it indicates that, for the time being, there's nothing more to clean.
     *
     * @return the number of cleaned Cleaners so far
     */
    public static int assistCleanup() {
        synchronized (handler.assistCleanupLock) {
            // CleanerHandler should not interfere while we're assisting, but the cleanersQueue
            // should not be locked either so that we don't block ReferenceHandler thread.
            // so we pause CleanerHandler by interrupting it...
            handler.interrupt();
            Cleaner c;
            boolean didSomeWork;
            do {
                didSomeWork = false;
                // 1st drain the cleanersQueue
                while ((c = (Cleaner) cleanersQueue.poll()) != null) {
                    c.clean();
                    didSomeWork = true;
                }
                // then steal any pending cleaners that are not yet enqueue-ed
                // (helping ReferenceHandler thread but bypassing enqueue-ing)
                Iterator<Reference<Object>> stolenCleaners =
                    Reference.stealPendingReferencesForQueue(cleanersQueue);
                // and process stolen cleaners too, if any
                if (stolenCleaners != null) {
                    while (stolenCleaners.hasNext()) {
                        ((Cleaner) stolenCleaners.next()).clean();
                    }
                    didSomeWork = true;
                }
            } while (didSomeWork);
        }
        // return accumulated clean count
        return cleanCount.get();
    }

    /**
     * Runs this cleaner, if it has not been run before.
     */
    public void clean() {
        if (!remove(this))
            return;
        try {
            thunk.run();
            cleanCount.incrementAndGet();
        } catch (final Throwable x) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        if (System.err != null)
                            new Error("Cleaner terminated abnormally", x)
                                .printStackTrace();
                        System.exit(1);
                        return null;
                    }});
        }
    }

}
