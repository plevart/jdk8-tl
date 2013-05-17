/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * @author peter
 */
public class InterruptReferenceHandlerTest {
    public static void main(String[] args) throws Exception {
        // wait for system background threads to load their classes
        Thread.sleep(1000L);
        System.out.println();
        System.out.println("START!");
        System.out.println();

        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (
            ThreadGroup tgn = tg;
            tgn != null;
            tg = tgn, tgn = tg.getParent()
            )
            ;

        Thread[] threads = new Thread[tg.activeCount()];
        Thread referenceHandlerThread = null;
        int n = tg.enumerate(threads);
        for (int i = 0; i < n; i++) {
            if ("Reference Handler".equals(threads[i].getName())) {
                referenceHandlerThread = threads[i];
            }
        }

        if (referenceHandlerThread == null) {
            System.out.println("No Reference Handler thread found");
        }
        else {
            System.out.println("Found: " + referenceHandlerThread);
        }
        Thread.sleep(1000L);
        System.out.println("Interrupting: " + referenceHandlerThread);
        referenceHandlerThread.interrupt();
        Thread.sleep(1000L);
        System.out.println("Interrupted: " + referenceHandlerThread);
        Thread.sleep(1000L);
    }
}
