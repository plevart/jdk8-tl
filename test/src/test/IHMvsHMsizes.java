package test;

/**
 */
public class IHMvsHMsizes {
    private static int hmCapacity(int initialCapacity) {
        int MAXIMUM_CAPACITY = 1 << 30;

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        return capacity;
    }

    private static int ihmCapacity(int expectedMaxSize) {
        int MINIMUM_CAPACITY = 4;
        int MAXIMUM_CAPACITY = 1 << 29;

        // Compute min capacity for expectedMaxSize given a load factor of 2/3
        int minCapacity = (3 * expectedMaxSize) / 2;

        // Compute the appropriate capacity
        int result;
        if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
            result = MAXIMUM_CAPACITY;
        } else {
            result = MINIMUM_CAPACITY;
            while (result < minCapacity)
                result <<= 1;
        }
        return result;
    }

    static String calc(int size, boolean x64) {
        int hmCap = Math.max(16, hmCapacity(4 * size / 3));
        int ihmCap = Math.max(8, ihmCapacity(size));

        int hmSize = x64 ? 16 + 4 * 8 + 5 * 4 + 4 : 8 + 4 * 4 + 5 * 4 + 4;
        int ihmSize = x64 ? 16 + 4 * 8 + 3 * 4 + 4 : 8 + 4 * 4 + 3 * 4 + 4;

        int hmValuesSize = x64 ? 16 + 8 : 8 + 4 + 4;
        int ihmValuesSize = hmValuesSize;

        int hmArraySize = x64 ? 24 + hmCap * 8 : 12 + hmCap * 4 + ((hmCap + 1) % 2) * 4;
        int ihmArraySize = x64 ? 24 + ihmCap * 2 * 8 : 12 + ihmCap * 2 * 4 + 4;

        int hmEntriesSize = size * (x64 ? 16 + 3 * 8 + 4 + 4 : 8 + 3 * 4 + 4);
        int ihmEntriesSize = 0;

        int hmBytes = hmSize + hmValuesSize + hmArraySize + hmEntriesSize;
        int ihmBytes = ihmSize + ihmValuesSize + ihmArraySize + ihmEntriesSize;

        return String.format(
            "%8d|%8d %8d|%8d %8d|%8d",
            size,
            hmCap,
            hmBytes,
            ihmCap,
            ihmBytes,
            ihmBytes - hmBytes
        );
    }

    public static void main(String[] args) {
        System.out.println("\n32 bit JVM:\n");
        System.out.println("        |     HashMap     | IdentityHashMap |");
        System.out.println("    size|capacity    bytes|capacity    bytes|IHM.bytes-HM.bytes");
        System.out.println("--------+-----------------+-----------------+------------------");
        for (int i = 0; i < 20; i++)  System.out.println(calc(i, false));
        for (int i = 20; i < 200; i+=20)  System.out.println(calc(i, false));

        System.out.println("\n64 bit JVM:\n");
        System.out.println("        |     HashMap     | IdentityHashMap |");
        System.out.println("    size|capacity    bytes|capacity    bytes|IHM.bytes-HM.bytes");
        System.out.println("--------+-----------------+-----------------+------------------");
        for (int i = 0; i < 20; i++)  System.out.println(calc(i, true));
        for (int i = 20; i < 200; i+=20)  System.out.println(calc(i, true));
    }
}
