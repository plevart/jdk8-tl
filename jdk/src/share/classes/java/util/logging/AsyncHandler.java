/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package java.util.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AsyncHandler extends Handler {
    final BlockingQueue<LogRecord> queue = createBlockingQueue();
    final Handler downstreamHandler = createDownstreamHandler();
    private final AtomicInteger overflowCounter = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AsyncHandler() {
        new PublisherThread().start();
    }

    protected BlockingQueue<LogRecord> createBlockingQueue() {
        // could give some capacity limit if needed.
        // if queue is filled, further records are dropped, but when
        // there's more room available, this fact is logged...
        return new LinkedBlockingQueue<>();
    }

    protected abstract Handler createDownstreamHandler();


    @Override
    public void publish(LogRecord record) {
        if (!queue.offer(record)) {
            overflowCounter.incrementAndGet();
        } else {
            int overflowed = overflowCounter.getAndSet(0);
            if (overflowed > 0) {
                LogRecord overflowNotification = new LogRecord(Level.WARNING,
                    overflowed + " log records overflowed!");
                overflowNotification.setLoggerName(AsyncHandler.class.getName());
                if (!queue.offer(overflowNotification)) {
                    overflowCounter.addAndGet(overflowed);
                }
            }
        }
    }

    @Override
    public void flush() {
        FlushRecord flushRecord = new FlushRecord();
        queue.add(flushRecord);
        flushRecord.awaitAcknowledge();
    }

    @Override
    public void close() throws SecurityException {
        checkPermission();
        if (!closed.getAndSet(true)) {
            CloseRecord closeRecord = new CloseRecord();
            queue.add(closeRecord);
            closeRecord.awaitAcknowledge();
        }
    }

    private static final AtomicInteger seq = new AtomicInteger();

    private class PublisherThread extends Thread {
        PublisherThread() {
            super("AsyncHandlerPublisher-thread-" + seq.incrementAndGet());
            setDaemon(true);
        }

        @Override
        public void run() {
            List<QuiesceRecord> pendingQuiesce = new ArrayList<>();

            while (true) {
                LogRecord record = queue.poll();
                if (record == null) { // queue drained -> process pending quiesce records
                    if (!pendingQuiesce.isEmpty()) {
                        boolean flushed = false;
                        boolean closed = false;
                        for (QuiesceRecord qr : pendingQuiesce) {
                            RuntimeException exception = null;
                            try {
                                if (qr instanceof FlushRecord && !flushed && !closed) {
                                    downstreamHandler.flush();
                                    flushed = true;
                                } else if (qr instanceof CloseRecord && !closed) {
                                    downstreamHandler.close();
                                    closed = true;
                                }
                            } catch (RuntimeException e) {
                                exception = e;
                            }
                            qr.signalAcknowledge(exception);
                        }
                        pendingQuiesce.clear();
                        if (closed) { // terminate publisher thread
                            break;
                        }
                    }
                    // wait for next record...
                    try {
                        record = queue.take();
                    } catch (InterruptedException e) {
                        // ignore
                        continue;
                    }
                }
                assert record != null;
                if (record instanceof QuiesceRecord) {
                    pendingQuiesce.add((QuiesceRecord) record);
                } else {
                    downstreamHandler.publish(record);
                }
            }
        }
    }

    // special command record that is executed only after queue is quiesced
    // supporting acknowledgments and exceptions...
    private static class QuiesceRecord extends LogRecord {
        QuiesceRecord() {
            super(Level.ALL, "");
        }

        private boolean acknowledged;
        private RuntimeException exception;

        synchronized void awaitAcknowledge() throws RuntimeException {
            boolean interrupted = false;
            while (!acknowledged) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            if (exception != null) {
                throw exception;
            }
        }

        synchronized void signalAcknowledge(RuntimeException exception) {
            this.exception = exception;
            this.acknowledged = true;
            notify(); // at most one waiter
        }
    }

    private static class FlushRecord extends QuiesceRecord {}

    private static class CloseRecord extends QuiesceRecord {}
}
