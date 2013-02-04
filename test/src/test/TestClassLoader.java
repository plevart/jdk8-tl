package test;

import si.pele.microbench.SizeOf;
import sun.misc.LockMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 */
public class TestClassLoader {
    static void testCl() throws InterruptedException, ClassNotFoundException {
        System.out.println("main()");
        Thread.sleep(1000L);
        System.out.println("gc()");
        System.gc();
        Class.forName("javax.swing.JComponent");
        Thread.sleep(1000L);
        System.out.println("gc()");
        System.gc();
        Class.forName("org.w3c.dom.Document");
        Thread.sleep(1000L);
        System.out.println("gc()");
        System.gc();
        Class.forName("javax.xml.bind.JAXBContext");
        System.out.println("end.");
    }

    static List<String> getRtClassNames() throws IOException {
        String rtJar = System.getProperty("java.home") + "/lib/rt.jar";
        JarInputStream jarStream = new JarInputStream(new FileInputStream(rtJar));
        JarEntry entry;
        List<String> classNames = new ArrayList<>();
        while ((entry = jarStream.getNextJarEntry()) != null) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class"))
                classNames.add(name.substring(0, name.length() - 6).replace('/', '.'));
        }
        return classNames;
    }

    static int testLoadRtClasses() throws IOException {

        int count = 0;

        for (String className : getRtClassNames()) {
            try {
                //System.out.println("Atempting load of class: " + className);
                Class.forName(className);
                count++;
            }
            catch (Throwable t) {
                //System.out.println("Unsuccessful loading of class: " + className + ": " + t.getMessage());
                count++;
            }
        }

        return count;
    }

    static long dumpSizes(long referenceSize) {
        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);
        ClassLoader cl0 = TestClassLoader.class.getClassLoader().getParent();
        ClassLoader cl1 = TestClassLoader.class.getClassLoader();
        long cl0size = sizeOf.deepSizeOf(cl0);
        long cl1size = sizeOf.deepSizeOf(cl1);
        System.out.println("Total memory: " + Runtime.getRuntime().totalMemory() + " bytes");
        System.out.println("Free  memory: " + Runtime.getRuntime().freeMemory() + " bytes");
        System.out.println("Deep size of " + cl0 + ": " + cl0size + " bytes");
        System.out.println("Deep size of " + cl1 + ": " + cl1size + " bytes");
        long totalSize = cl0size + cl1size;
        System.out.println("Deep size of both: " + totalSize + " bytes" + (referenceSize == 0L ? " (reference)" : " (difference to reference: " + (totalSize - referenceSize) + " bytes)"));
        System.out.println("Lock stats...\n" + LockMap.getAndResetStats());
        return totalSize;
    }

    static class Last {}

    public static void main(String[] args) throws Exception {

        System.out.println("\n...At the beginning of main()\n");

        long size0 = dumpSizes(0L);

        long t0 = System.nanoTime();
        int classes = testLoadRtClasses();
        double t = (double)(System.nanoTime() - t0)/1000000d;
        System.out.println("\n...Attempted to load: " + classes + " classes in: " + t + " ms\n");

        dumpSizes(size0);

        System.out.println("\n...Performing gc()");
        System.gc();
        Thread.sleep(500L);

        System.out.println("\n...Loading class: " + Last.class.getName() + " (to trigger expunging)\n");

        dumpSizes(size0);
    }
}
