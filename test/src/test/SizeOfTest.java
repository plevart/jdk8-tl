package test;

import si.pele.microbench.SizeOf;

/**
 */
public class SizeOfTest {
    public static void main(String[] args) {
        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.STDOUT);
        sizeOf.deepSizeOf(SizeOfTest.class.getClassLoader());
    }
}
