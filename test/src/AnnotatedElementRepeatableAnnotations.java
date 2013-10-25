/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

/**
 * @author peter
 */
public class AnnotatedElementRepeatableAnnotations {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(AnnCont.class)
    @interface Ann {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface AnnCont {
        Ann[] value();
    }

    @Ann(1) @AnnCont({@Ann(2), @Ann(3)}) static class A {}
    @AnnCont({@Ann(1), @Ann(2)}) @Ann(3) static class B {}
    @AnnCont({@Ann(1), @Ann(2), @Ann(3)}) static class C {}
    @Ann(1) @Ann(2) @Ann(3) static class D {}
    @Ann(1) static class E {}
    static class F {}

    static final Class<?>[] classes = {A.class, B.class, C.class, D.class, E.class, F.class};

    static class AnnotatedElementProxy implements AnnotatedElement {

        private final AnnotatedElement delegate;

        AnnotatedElementProxy(AnnotatedElement delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return delegate.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {return delegate.getAnnotations();}

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return delegate.getDeclaredAnnotations();
        }

        @Override
        public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
            return delegate.getDeclaredAnnotation(annotationClass);
        }
    }

    public static void main(String[] args) {
        for (Class<?> clazz : classes) {
            AnnotatedElement ae = new AnnotatedElementProxy(clazz);
            System.out.println(clazz + ": " + Arrays.toString(ae.getDeclaredAnnotationsByType(Ann.class)));
        }
    }
}


