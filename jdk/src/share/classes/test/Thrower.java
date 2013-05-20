package test;

public class Thrower {

    static class ExceptionA extends RuntimeException {
        static {
            if (true) throw new OutOfMemoryError("Error initializing ExceptionA");
        }
    }

    static class ExceptionB extends RuntimeException {}

    public void throwCatchExceptionA() {
        try {
            throw new ExceptionA();
        } catch (ExceptionA x) {}
          catch (OutOfMemoryError x) {
            x.printStackTrace(System.out);
        }

    }
}
