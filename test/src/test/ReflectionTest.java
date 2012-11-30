package test;

import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class ReflectionTest
{
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface InheritedAnn
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann1
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann2
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann3
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann4
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann5
    {
        String value();
    }

    @InheritedAnn("A")
    @Ann1("A")
    @Ann2("A")
    @Ann3("A")
    @Ann4("A")
    @Ann5("A")
    public static class ClassA
    {
        @Ann1("A.f1")
        @Ann2("A.f1")
        @Ann3("A.f1")
        @Ann4("A.f1")
        @Ann5("A.f1")
        public String f1;

        @Ann1("A.<init>")
        @Ann2("A.<init>")
        @Ann3("A.<init>")
        @Ann4("A.<init>")
        @Ann5("A.<init>")
        public ClassA()
        {
        }

        @Ann1("A.m1")
        @Ann2("A.m1")
        @Ann3("A.m1")
        @Ann4("A.m1")
        @Ann5("A.m1")
        public void m1()
        {
        }

        @Ann1("A.m2")
        @Ann2("A.m2")
        @Ann3("A.m2")
        @Ann4("A.m2")
        @Ann5("A.m2")
        public void m2()
        {
        }
    }

    @Ann1("B")
    @Ann2("B")
    @Ann3("B")
    @Ann4("B")
    @Ann5("B")
    public static class ClassB extends ClassA
    {
        @Ann1("B.f1")
        @Ann2("B.f1")
        @Ann3("B.f1")
        @Ann4("B.f1")
        @Ann5("B.f1")
        public String f1;

        @Ann1("B.<init>")
        @Ann2("B.<init>")
        @Ann3("B.<init>")
        @Ann4("B.<init>")
        @Ann5("B.<init>")
        public ClassB()
        {
        }

        @Ann1("B.m1")
        @Ann2("B.m1")
        @Ann3("B.m1")
        @Ann4("B.m1")
        @Ann5("B.m1")
        public void m1()
        {
        }

        @Ann1("B.m2")
        @Ann2("B.m2")
        @Ann3("B.m2")
        @Ann4("B.m2")
        @Ann5("B.m2")
        public void m2()
        {
        }
    }

    @Ann1("C")
    @Ann2("C")
    @Ann3("C")
    @Ann4("C")
    @Ann5("C")
    public static class ClassC extends ClassB
    {
        @Ann1("C.f1")
        @Ann2("C.f1")
        @Ann3("C.f1")
        @Ann4("C.f1")
        @Ann5("C.f1")
        public String f1;

        @Ann1("C.<init>")
        @Ann2("C.<init>")
        @Ann3("C.<init>")
        @Ann4("C.<init>")
        @Ann5("C.<init>")
        public ClassC()
        {
        }

        @Ann1("C.m1")
        @Ann2("C.m1")
        @Ann3("C.m1")
        @Ann4("C.m1")
        @Ann5("C.m1")
        public void m1()
        {
        }

        @Ann1("C.m2")
        @Ann2("C.m2")
        @Ann3("C.m2")
        @Ann4("C.m2")
        @Ann5("C.m2")
        public void m2()
        {
        }
    }

    static void dump(Annotation[] annotations, String prefix, Appendable sb) throws Exception
    {
        for (Annotation ann : annotations)
        {
            String value = (String) ann.annotationType().getMethod("value", new Class[0]).invoke(ann, (Object[]) null);
            sb.append(prefix).append("@").append(ann.annotationType().getName()).append("(\"").append(value).append("\")\n");
        }
    }

    static void dump(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods, Appendable sb)
    {
        try
        {
            dump(clazz.getAnnotations(), "", sb);
            sb.append("class ").append(clazz.getName()).append(" {\n\n");

            if (fields != null)
            {
                for (Field f : fields)
                {
                    dump(f.getAnnotations(), "  ", sb);
                    sb.append("  ").append(f.toGenericString()).append(";\n\n");
                }
            }

            if (methods != null)
            {
                for (Constructor c : constructors)
                {
                    dump(c.getAnnotations(), "  ", sb);
                    sb.append("  ").append(c.toGenericString()).append(";\n\n");
                }
            }

            if (methods != null)
            {
                for (Method m : methods)
                {
                    dump(m.getAnnotations(), "  ", sb);
                    sb.append("  ").append(m.toGenericString()).append(";\n\n");
                }
            }

            sb.append("}\n\n");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static final Object NOT_ANNOTATION = new Object();
    
    static void test(Annotation ann)
    {
        if (ann == NOT_ANNOTATION) {
            throw new AssertionError();
        }
    }
    
    static void test(Class<?> clazz)
    {
        test(clazz.getAnnotation(InheritedAnn.class));
        test(clazz.getAnnotation(Ann1.class));
    }

    static void test(Class<?> clazz, Field[] fields, Constructor[] constructors, Method[] methods)
    {
        test(clazz);

        for (Field f : fields)
        {
            test(f.getAnnotation(Ann1.class));
        }


        for (Constructor c : constructors)
        {
            test(c.getAnnotation(Ann1.class));
        }

        for (Method m : methods)
        {
            test(m.getAnnotation(Ann1.class));
        }
    }

    static class Test1 extends Thread
    {
        final int loops;

        Test1(int loops)
        {
            this.loops = loops;
        }

        @Override
        public void run()
        {
            for (int i = 0; i < loops; i++)
            {
                test(ClassA.class, ClassA.class.getFields(), ClassA.class.getConstructors(), ClassA.class.getMethods());
                test(ClassB.class, ClassB.class.getFields(), ClassB.class.getConstructors(), ClassB.class.getMethods());
                test(ClassC.class, ClassC.class.getFields(), ClassC.class.getConstructors(), ClassC.class.getMethods());
            }
        }
    }

    static class Test2 extends Thread
    {
        final int loops;

        Test2(int loops)
        {
            this.loops = loops;
        }

        @Override
        public void run()
        {
            Field[] classAfields = ClassA.class.getFields();
            Constructor[] classAconstructors = ClassA.class.getConstructors();
            Method[] classAmethods = ClassA.class.getMethods();

            Field[] classBfields = ClassB.class.getFields();
            Constructor[] classBconstructors = ClassB.class.getConstructors();
            Method[] classBmethods = ClassB.class.getMethods();

            Field[] classCfields = ClassC.class.getFields();
            Constructor[] classCconstructors = ClassC.class.getConstructors();
            Method[] classCmethods = ClassC.class.getMethods();

            for (int i = 0; i < loops; i++)
            {
                test(ClassA.class, classAfields, classAconstructors, classAmethods);
                test(ClassB.class, classBfields, classBconstructors, classBmethods);
                test(ClassC.class, classCfields, classCconstructors, classCmethods);
            }
        }
    }

    static class Test3 extends Thread
    {
        final int loops;

        Test3(int loops)
        {
            this.loops = loops;
        }

        @Override
        public void run()
        {
            for (int i = 0; i < loops; i++)
            {
                test(ClassA.class);
                test(ClassB.class);
                test(ClassC.class);
            }
        }
    }

    static void testCorrectness()
    {
        StringBuilder sb = new StringBuilder();
        dump(ClassA.class, ClassA.class.getFields(), ClassA.class.getConstructors(), ClassA.class.getMethods(), sb);
        dump(ClassB.class, ClassB.class.getFields(), ClassB.class.getConstructors(), ClassB.class.getMethods(), sb);
        dump(ClassC.class, ClassC.class.getFields(), ClassC.class.getConstructors(), ClassC.class.getMethods(), sb);

        System.out.println(sb);
    }

    static long test1(int threads, int loops, long prevT)
    {

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = new Test1(loops);
        }

        return runWorkers(workers, loops, prevT);
    }

    static long test2(int threads, int loops, long prevT)
    {

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = new Test2(loops);
        }

        return runWorkers(workers, loops, prevT);
    }

    static long test3(int threads, int loops, long prevT)
    {

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = new Test3(loops);
        }

        return runWorkers(workers, loops, prevT);
    }

    static long runWorkers(Thread[] workers, int loops, long prevT)
    {

        try
        {
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
            System.gc();
            Thread.sleep(500L);
        }
        catch (InterruptedException e)
        {
        }

        long t0 = System.nanoTime();

        for (int i = 0; i < workers.length; i++)
        {
            workers[i].start();
        }

        for (int i = 0; i < workers.length; i++)
        {
            try
            {
                workers[i].join();
            }
            catch (InterruptedException e)
            {
            }
        }

        long t = System.nanoTime() - t0;

        System.out.println(
                workers[0].getClass().getSimpleName() + ": "
                + String.format("%3d", workers.length) + " concurrent threads * "
                + String.format("%9d", loops) + " loops each: "
                + String.format("%,15.3f", (double) t / 1000000d) + " ms"
                + (prevT == 0L ? "" : String.format(" (x %6.2f)", (double) t / (double) prevT)));

        return t;
    }

    public static void main(String[] args) throws IOException
    {
        System.out.println();

        long t;
        System.out.println("warm-up:");
        t = test1(1, 20000, 0L);
        test1(1, 20000, t);
        test1(1, 20000, t);
        System.out.println();
        t = test2(1, 2000000, 0);
        test2(1, 2000000, t);
        test2(1, 2000000, t);
        System.out.println();
        t = test3(1, 10000000, 0);
        test3(1, 10000000, t);
        test3(1, 10000000, t);
        System.out.println();

        System.out.println("measure:");
        t = test1(1, 20000, 0);
        test1(2, 20000, t);
        test1(4, 20000, t);
        test1(8, 20000, t);
        test1(32, 20000, t);
        test1(128, 20000, t);
        System.out.println();
        t = test2(1, 2000000, 0);
        test2(2, 2000000, t);
        test2(4, 2000000, t);
        test2(8, 2000000, t);
        test2(32, 2000000, t);
        test2(128, 2000000, t);
        System.out.println();
        t = test3(1, 10000000, 0);
        test3(2, 10000000, t);
        test3(4, 10000000, t);
        test3(8, 10000000, t);
        test3(32, 10000000, t);
        test3(128, 10000000, t);
        System.out.println();
    }
}
