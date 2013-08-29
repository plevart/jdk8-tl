import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class toArray {

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 100; i++) {
            main();
        }
    }

    static int sizeOf(Iterator<?> iter) {
        int n = 0;
        while (iter.hasNext()) {
            n++;
            iter.next();
        }
        return n;
    }

    public static void main() throws Throwable {
        final Throwable throwable[] = new Throwable[1];
        final ConcurrentHashMap<Integer, Integer> m
            = new ConcurrentHashMap<Integer, Integer>(1);

        BiConsumer<Integer, Integer> r = (o, b) -> {
            for (int i = o; i < b; i++)
                m.put(i, i);
        };

        final int nWorkers = 2;
        final int sizePerWorker = 1024;
        final int maxSize = nWorkers * sizePerWorker;
        List<Thread> workers = IntStream.range(0, nWorkers).
            map(w -> w * sizePerWorker).
            mapToObj(w -> (Runnable )() -> r.accept(w, w + sizePerWorker)).
            map(Thread::new).collect(Collectors.<Thread>toList());

        class Change {
            final Iterator<?> prevIterator, nextIterator;
            final int prevSize, nextSize;

            Change(Iterator<?> prevIterator, int prevSize, Iterator<?> nextIterator, int nextSize) {
                this.prevIterator = prevIterator;
                this.prevSize = prevSize;
                this.nextIterator = nextIterator;
                this.nextSize = nextSize;
            }

            @Override
            public String toString() {
                return "prevIterator(iterations="+ prevSize + "): " + prevIterator + "\n" +
                       "nextIterator(iterations="+ nextSize + "): " + nextIterator;
            }
        }

        final List<Change> wrongWayChanges = new ArrayList<>();

        final Thread foreman = new Thread() {
            private Iterator<?> prevIterator = Collections.emptyIterator();
            private int prevSize = 0;

            private boolean checkProgress(Iterator<?> iterator) {
                int size = sizeOf(iterator);
                if (size < prevSize) wrongWayChanges.add(new Change(prevIterator, prevSize, iterator, size));
                if (size > maxSize)  throw new RuntimeException("OVERSHOOT");
                if (size == maxSize) return true;
                prevIterator = iterator;
                prevSize = size;
                return false;
            }

            public void run() {
                try {
                    Integer[] empty = new Integer[0];
                    while (true) {
                        if (checkProgress(m.values().iterator())) return;
                    }
                } catch (Throwable t) {
                    throwable[0] = t;
                }}};

        foreman.start();
        workers.stream().forEach(Thread::start);

        workers.stream().forEach(toArray::join);
        foreman.join();

        for (Change change : wrongWayChanges) {
            System.out.println(change.toString());
        }

        if (throwable[0] != null)
            throw throwable[0];
    }

    static void join(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
