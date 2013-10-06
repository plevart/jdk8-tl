import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public class DirectBufferTest {
    private static int NUM_THREADS = 256;
    private static int ALLOC_CAPACITY_MIN = 1*1024;
    private static int ALLOC_CAPACITY_MAX = 1024*1024;
    private static int TIME_MEASURE_CHUNK = 500;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread("thread-" + i) {
                public void run() {
                    int it = 0;
                    try {
                        long t0 = System.nanoTime();
                        for (;;) {
                            for (int i = 0; i < TIME_MEASURE_CHUNK; i++) {
                                ByteBuffer.allocateDirect(
                                    ThreadLocalRandom.current()
                                                     .nextInt(ALLOC_CAPACITY_MIN, ALLOC_CAPACITY_MAX+1)
                                );
                                it++;
                            }
                            long t1 = System.nanoTime();
                            System.err.printf("%10s: %5.2f ms/op\n",
                                              getName(),
                                              ((double)(t1 - t0)/(1_000_000d*TIME_MEASURE_CHUNK)));
                            t0 = t1;
                        }
                    }
                    catch (OutOfMemoryError t) {
                        System.err.println(Thread.currentThread().getName() +
                                           " got an OOM on iteration " + it);
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