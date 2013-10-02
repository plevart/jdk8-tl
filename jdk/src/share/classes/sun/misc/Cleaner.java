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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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
 * @author Aleksey Shipilev
 */

public class Cleaner extends WeakReference<Object> {

    /*
     * Implementation notes:
     *
     * Cleaner has multiple ways to trigger the cleanup:
     *
     * a) CleanupHandler blocking-waits on the reference queue, and triggers cleanups
     *    as soon as it gets enqueued on the QUEUE; this limits the speed of deallocation
     *    by the performance of both CleanupHandler thread, and also the ReferenceHandler
     *    thread.
     *
     * b) Allocators can call assistCleanup() to help with draining the reference queue.
     *    It is usually not required to call assistCleanup() from anywhere except create()
     *    here.  This decouples us from CleanupHandler, but still takes us at the mercy of
     *    ReferenceHandler thread.
     *
     * c) The housekeeping code may invoke assistCleanupSlow() to force traversal of our Cleaner
     *    storage, and figure out if we need to clean up something else. This decouples us
     *    from both CleanupHandler and ReferenceHandler threads. Note that assistCleanupSlow()
     *    requires the detection if we had been cleared. That is why Cleaner is WeakReference,
     *    not the PhantomReference: we need (referent == null) as the signal we are cleared.
     */

    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();
    private static final Set<Cleaner> CLEANERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static {
        Thread handler = new Thread(new CleanupHandler());
        handler.setName("CleanupHandler");
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);
        handler.start();
    }

    /**
     * CleanupHandler thread pumps up the queue when no one is present
     * to assist the cleanup.
     */
    public static class CleanupHandler implements Runnable {
        @Override
        public void run() {
            try {
                Reference ref;
                while ((ref = QUEUE.remove()) != null) {
                    Cleaner h = (Cleaner) ref;
                    h.clean();
                }
            } catch (InterruptedException e) {
                // Restore interrupt status and exit
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Assist the cleanup
     */
    public static void assistCleanup() {
        Reference ref;
        while ((ref = QUEUE.poll()) != null) {
            Cleaner h = (Cleaner) ref;
            h.clean();
        }
    }

    /**
     * Assist the cleanup
     * @param max max elements to clean up
     */
    public static void assistCleanup(int max) {
        int budget = max;
        Reference ref;
        while ((budget-- > 0) && (ref = QUEUE.poll()) != null) {
            Cleaner h = (Cleaner) ref;
            h.clean();
        }
    }

    /**
     * Do the slower cleanup.
     */
    public static void assistCleanupSlow() {
        Collection<Cleaner> toPurge = new ArrayList<>();
        for (Cleaner c : CLEANERS) {
            if (c.get() == null) {
                toPurge.add(c);
            }
        }

        for (Cleaner c : toPurge) {
            c.clean();
        }
    }

    /**
     * Cleanup handler
     */
    private final Runnable thunk;

    private Cleaner(Object referent, Runnable thunk) {
        super(referent, QUEUE);
        CLEANERS.add(this);
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
        // Clean up the slot for us, and also do some charity work;
        // the additional charity work will help to converge even
        // when the CleanupHandler is stuck

        if (thunk == null) {
            assistCleanup(1);
            return null;
        } else {
            assistCleanup(2);
            return new Cleaner(ob, thunk);
        }
    }

    /**
     * Runs this cleaner, if it has not been run before.
     */
    public void clean() {
        if (!CLEANERS.remove(this)) {
            // already cleaned up
            return;
        }

        try {
            thunk.run();
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
