/**
 * Written by Peter.Levart@gmail.com 
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author peter
 */
public class AllocTest {

    static final Unsafe unsafe;

    static {
        Unsafe u = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = (Unsafe) f.get(null);
        }
        catch (Exception x) { }
        unsafe = u;
    }


    static long allocateMemory(long bytes) {
        return unsafe.allocateMemory(bytes);
    }

    static void freeMemory(long address) {
        unsafe.freeMemory(address);
    }

    static final BlockingQueue<Long> addrsQueue = new ArrayBlockingQueue<Long>(100);

    static class Deallocator implements Callable<Long> {
        @Override
        public Long call() {
            long sumTime = 0L;
            try {
                long addr;
                while ((addr = addrsQueue.take()) != 0L) {
                    long t0 = System.nanoTime();
                    freeMemory(addr);
                    sumTime += System.nanoTime() - t0;
                }
            }
            catch (InterruptedException e) { }
            return sumTime;
        }
    }

    static class Allocator implements Callable<Long> {
        final int blocks;
        final long blockSize;

        Allocator(int blocks, long blockSize) {
            this.blocks = blocks;
            this.blockSize = blockSize;
        }

        @Override
        public Long call() {
            long sumTime = 0L;
            try {
                for (int i = 0; i < blocks; i++) {
                    long t0 = System.nanoTime();
                    long addr = allocateMemory(blockSize);
                    sumTime += System.nanoTime() - t0;
                    addrsQueue.put(addr);
                }
            }
            catch (InterruptedException e) { }
            return sumTime;
        }
    }

    static final ExecutorService pool = Executors.newCachedThreadPool();

    static void doTest(int allocators, int blocks, long blockSize, boolean print) throws Exception {
        Future<Long> deallocTimeF = pool.submit(new Deallocator());
        Future<Long>[] allocTimesF = new Future[allocators];
        for (int i = 0; i < allocators; i++) {
            allocTimesF[i] = pool.submit(new Allocator(blocks, blockSize));
        }
        double deallocTime;
        double allocTime = 0;
        double allocTimes[] = new double[allocators];
        for (int i = 0; i < allocators; i++) {
            allocTime += (allocTimes[i] = allocTimesF[i].get().doubleValue()/1000_000d);
        }
        addrsQueue.put(0L); // signal end
        deallocTime = deallocTimeF.get().doubleValue()/1000_000d;
        if (print) {
        System.out.println("allocators: " + allocators +
                           ", blocks per allocator: " + blocks +
                           ", total blocks: " + (allocators*blocks) +
                           ", block size: " + blockSize);
        System.out.println("  deallocation time: " + deallocTime + " ms");
        System.out.println("    allocation time: " + allocTime + " ms - " + Arrays.toString(allocTimes));
        System.out.println("-----------");
        }
    }

    public static void main(String[] args) throws Exception {
        long blockSize = 2000_000L;

        doTest(1, 1000000, blockSize, false);
        doTest(1, 1000000, blockSize, false);
        doTest(1, 1000000, blockSize, false);

        doTest(1, 1000000, blockSize, true);
        doTest(2, 500000, blockSize, true);
        doTest(4, 250000, blockSize, true);
        doTest(8, 125000, blockSize, true);
        doTest(16, 62500, blockSize, true);
        pool.shutdown();
    }
}
