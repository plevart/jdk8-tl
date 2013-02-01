package test;

@RuntimeAnnotationA
public class AnnotationRetentionTest {
    public static void main(String[] args) {
        RuntimeAnnotationA ann1 = AnnotationRetentionTest.class.getDeclaredAnnotation(RuntimeAnnotationA.class);
        System.out.println(ann1 != null);
        RuntimeAnnotationA ann2 = RuntimeAnnotationB.class.getDeclaredAnnotation(RuntimeAnnotationA.class);
        System.out.println(ann2 != null);
    }
}