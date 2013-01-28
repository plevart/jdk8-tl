package test;

import si.pele.microbench.SizeOf;

/**
 */
public class SizeOfTest {
    public static void main(String[] args) {
        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.STDOUT);
        System.out.println(sizeOf.sizeOf(SizeOfTest.class.getClassLoader()));
        System.out.println(sizeOf.sizeOf(SizeOfTest.class.getClassLoader().getParent()));
    }
}
