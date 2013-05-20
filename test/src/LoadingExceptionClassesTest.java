
public class LoadingExceptionClassesTest {

    public static void main(String[] args) throws Exception {
        // wait for system background threads to load their classes
        Thread.sleep(1000L);
        System.out.println();
        System.out.println("START!");
        System.out.println();

        String throwerClassName = "test.Thrower";

        System.out.println(throwerClassName + " loading");
        Class.forName(throwerClassName, false, LoadingExceptionClassesTest.class.getClassLoader());
        System.out.println(throwerClassName + " loaded");

        System.out.println(throwerClassName + " initializing");
        Class.forName(throwerClassName, true, LoadingExceptionClassesTest.class.getClassLoader());
        System.out.println(throwerClassName + " initialized");

        System.out.println("constructing instance of " + throwerClassName);
        test.Thrower thrower = new test.Thrower();
        System.out.println("constructed " + thrower);

        System.out.println("calling " + throwerClassName + ".throwCatchExceptionA() 1st time");
        thrower.throwCatchExceptionA();
        System.out.println("returned from " + throwerClassName + ".throwCatchExceptionA()");

        System.out.println("calling " + throwerClassName + ".throwCatchExceptionA() 2nd time");
        thrower.throwCatchExceptionA();
        System.out.println("returned from " + throwerClassName + ".throwCatchExceptionA()");
    }
}
