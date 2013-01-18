package sun.reflect.annotation;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A concrete implementation of {@link UniqueIndex} mapping annotation types to annotations.<p>
 * See also convenience methods {@link #toArray()} and {@link #getAnnotation(Class)}.
 */
public class AnnotationMap extends UniqueIndex<Class<? extends Annotation>, Annotation> {

    /**
     * Creates an immutable map mapping annotation types to annotations.
     *
     * @param annotations A collection of annotations to construct an immutable map from.
     * @throws NonUniqueKeyException If not all annotations have unique annotation types.
     * @throws NullPointerException  If given collection or any of the elements of collection is null.
     */
    public AnnotationMap(Collection<? extends Annotation> annotations) throws NonUniqueKeyException, NullPointerException {
        super(annotations);
    }

    @Override
    protected Class<? extends Annotation> extractKey(Annotation annotation) {
        return annotation.annotationType();
    }

    /**
     * Returns an annotation of the give type or null if not found.
     *
     * @param annotationType the type of annotation requested.
     * @param <A>            the type parameter for the annotation type
     * @return annotation if found or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) super.get(annotationType);
    }

    /**
     * @return new array filled with annotations from this map
     */
    public Annotation[] toArray() {
        return super.toValuesArray(Annotation.class);
    }
}
