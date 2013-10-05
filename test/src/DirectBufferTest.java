import java.nio.ByteBuffer;

public class DirectBufferTest {
    private static int NUM_THREADS = 4;
    private static int TIME_MEASURE_CHUNK = 1000;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread("thread-" + i) {
                public void run() {
                    int it = 0;
                    long t0 = System.nanoTime();
                    try {
                        for (;;) {
                            for (int i = 0; i < TIME_MEASURE_CHUNK; i++) {
                                ByteBuffer bb = ByteBuffer.allocateDirect(1024 * 1024);
                                it++;
                            }
                            long t1 = System.nanoTime();
                            System.err.printf("%10s: %5.2f ms/op\n", getName(), ((double)(t1 - t0)/(1000_000d*TIME_MEASURE_CHUNK)));
                            t0 = t1;
                        }
                    }
                    catch (OutOfMemoryError t) {
                        System.err.println("Thread " + Thread.currentThread().getName() + " got an OOM on iteration " + it);
                        t.printStackTrace();
                        System.exit(1);
                    }
                }
            };
            thread.start();
        }

        Thread.sleep(300 * 1000);
        System.out.println("No errors after 60 seconds.");
        System.exit(0);
    }
}