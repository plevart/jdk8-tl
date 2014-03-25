/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntConsumer;

/**
 * ProcessReaper to wait for process exits and notify any interested party.
 *
 * @author rriggs
 */
public class ProcessReaper implements Runnable {

    static void debug(String pattern, Object... args) {
        if (false) {
            System.out.printf(pattern, args);
        }
    }

    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (
            ThreadGroup tgn = tg;
            tgn != null;
            tg = tgn, tgn = tg.getParent()
            )
            ;
        Thread reaper = new Thread(tg, new ProcessReaper(), "Process Reaper");
        reaper.setPriority(Thread.MAX_PRIORITY);
        reaper.setDaemon(true);
        reaper.start();
    }

    private static final ConcurrentMap<Integer, ProcessEntry> pidToEntryMap = new ConcurrentHashMap<>();

    private static class ProcessEntry {
        final IntConsumer exitValueConsumer;
        final int exitValue;
        final long registerTime = System.currentTimeMillis();

        ProcessEntry(IntConsumer exitValueConsumer) {
            this.exitValueConsumer = exitValueConsumer;
            this.exitValue = 0;
        }

        ProcessEntry(int exitValue) {
            this.exitValueConsumer = null;
            this.exitValue = exitValue;
        }

        boolean isExit() {
            return exitValueConsumer == null;
        }
    }

    /**
     * Queue up a callback when the specified process exits.
     *
     * @param pid               the process to wait for
     * @param exitValueConsumer the Consumer of the exitValue;
     *                          The callback must complete quickly to avoid blocking the reaper thread.
     */
    static void onExitCall(int pid, IntConsumer exitValueConsumer) {

        Integer pidKey = pid;
        ProcessEntry oldEntry = pidToEntryMap.get(pidKey);
        ProcessEntry newEntry = null;
        while (true) {
            if (oldEntry == null) { // no registration for pid yet
                if (newEntry == null)
                    newEntry = new ProcessEntry(exitValueConsumer);
                oldEntry = pidToEntryMap.putIfAbsent(pidKey, newEntry);
                if (oldEntry == null) {
                    break;
                }
            } else if (oldEntry.isExit()) { // exit value already registered
                if (pidToEntryMap.remove(pidKey, oldEntry)) {
                    exitValueConsumer.accept(oldEntry.exitValue);
                    break;
                } else {
                    oldEntry = pidToEntryMap.get(pidKey);
                }
            } else { // we have a consumer already registered
                throw new IllegalArgumentException("An exit value consumer for pid=" + pid + " is already registered");
            }
        }
    }

    /**
     * Wait for a process to exit and call its consumer with the exitValue.
     */
    @Override
    public void run() {

        while (true) {

            long result = waitForExit0();
            int exitPid = (int) (result >>> 32);
            int exitValue = (int) (result & 0xffffffff);
            if (exitPid <= 0) {
                // No child processes or some other wait pid error
                // Check that if there are any entries in the waitlist they are alive
                checkLiveness();
                continue;   // go back to the top
            }

            Integer pidKey = exitPid;
            ProcessEntry oldEntry = pidToEntryMap.get(pidKey);
            ProcessEntry newEntry = new ProcessEntry(exitValue);
            while (true) {
                if (oldEntry == null) { // no registration for pid yet
                    oldEntry = pidToEntryMap.putIfAbsent(pidKey, newEntry);
                    if (oldEntry == null) {
                        break;
                    }
                } else if (oldEntry.isExit()) { // exit value already registered
                    // this should not happen since waitForExit0 should report same pid only once
                    // but play nicely and just replace the exit value entry
                    if (pidToEntryMap.replace(pidKey, oldEntry, newEntry)) {
                        break;
                    } else {
                        oldEntry = pidToEntryMap.get(pidKey);
                    }
                } else { // a consumer is already registered
                    if (pidToEntryMap.remove(pidKey, oldEntry)) {
                        oldEntry.exitValueConsumer.accept(exitValue);
                        break;
                    } else {
                        oldEntry = pidToEntryMap.get(pidKey);
                    }
                }
            }
        }
    }

    /*
     * The exit value to insert when no exit value for a process is available. 
     */
    private static final int UNKNOWN_EXIT_VALUE = 256;

    /**
     * Check each consumer in the waitlist; remove and notify any dead ones.
     */
    private void checkLiveness() {
        Iterator<Map.Entry<Integer, ProcessEntry>> it = pidToEntryMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ProcessEntry> e = it.next();
            Integer pid = e.getKey();
            ProcessEntry entry = e.getValue();
            if (!isAlive0(pid)) {
                it.remove();
                if (!entry.isExit()) {
                    entry.exitValueConsumer.accept(UNKNOWN_EXIT_VALUE);
                }
            }
        }
    }

    /**
     * Native method to check if a process is alive
     *
     * @return
     */
    private static native boolean isAlive0(int pid);

    /**
     * Native invocation of waitPid.
     *
     * @return the pid in the high 32 bits and the exit code in the lower 32
     *         bits.
     */
    private static native long waitForExit0();
}
