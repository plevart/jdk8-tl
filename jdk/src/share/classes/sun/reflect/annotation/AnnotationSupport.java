/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.reflect.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Objects;

public final class AnnotationSupport {

    /**
     * Finds and returns all directly and indirectly present annotations
     * of a given annotated element.
     *
     * The order of the elements in the array returned is: first any
     * directly present annotation, followed by any in-directly present annotations
     * as declared inside container.
     *
     * @param annotatedElement the {@code AnnotatedElement} in which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty
     *         array if none were found
     */
    public static <A extends Annotation> A[] getDirectlyAndIndirectlyPresent(
            AnnotatedElement annotatedElement,
            Class<A> annoClass) {

        A directAnnotation = annotatedElement.getDeclaredAnnotation(annoClass);
        A[] indirectAnnotations = getIndirectlyPresent(annotatedElement, annoClass);
        if (directAnnotation == null) {
            if (indirectAnnotations == null) {
                return (A[]) Array.newInstance(annoClass, 0);
            } else {
                return indirectAnnotations;
            }
        } else {
            A[] allAnnotations = (A[]) Array.newInstance(annoClass,
                                                         indirectAnnotations == null
                                                         ? 1
                                                         : indirectAnnotations.length + 1);
            allAnnotations[0] = directAnnotation;
            if (indirectAnnotations != null && indirectAnnotations.length > 0) {
                System.arraycopy(indirectAnnotations, 0, allAnnotations, 1, indirectAnnotations.length);
            }
            return allAnnotations;
        }
    }

    /**
     * Finds and returns all annotations matching the given {@code annoClass}
     * indirectly present on {@code annotatedElement}.
     *
     * @param annotatedElement the {@code AnnotatedElement} in which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty array if no
     *         indirectly present annotations were found
     */
    private static <A extends Annotation> A[] getIndirectlyPresent(
            AnnotatedElement annotatedElement,
            Class<A> annoClass) {

        Repeatable repeatable = annoClass.getDeclaredAnnotation(Repeatable.class);
        if (repeatable == null)
            return null;  // Not repeatable -> no indirectly present annotations

        Class<? extends Annotation> containerClass = repeatable.value();

        Annotation container = annotatedElement.getDeclaredAnnotation(containerClass);
        if (container == null)
            return null;

        // Unpack container
        A[] valueArray = getValueArray(container);
        checkTypes(valueArray, container, annoClass);

        return valueArray;
    }

    /**
     * Finds and returns all associated annotations matching the given class.
     *
     * The order of the elements in the array returned is: first any
     * directly present annotation, followed by any in-directly present annotations
     * as declared inside container.
     *
     * @param declaringClass the declaring {@code Class} in which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty array if none were found.
     */
    public static <A extends Annotation> A[] getAssociatedAnnotations(
            Class<?> declaringClass,
            Class<A> annoClass) {
        Objects.requireNonNull(declaringClass);

        A[] result = null;
        for (Class decl = declaringClass; decl != null; decl = decl.getSuperclass()) {
            result = getDirectlyAndIndirectlyPresent(decl, annoClass);
            if (result.length > 0 ||
                !AnnotationType.getInstance(annoClass).isInherited()) {
                break;
            }
        }
        return result;
    }


    /* Reflectively invoke the values-method of the given annotation
     * (container), cast it to an array of annotations and return the result.
     */
    private static <A extends Annotation> A[] getValueArray(Annotation container) {
        try {
            // According to JLS the container must have an array-valued value
            // method. Get the AnnotationType, get the "value" method and invoke
            // it to get the content.

            Class<? extends Annotation> containerClass = container.annotationType();
            AnnotationType annoType = AnnotationType.getInstance(containerClass);
            if (annoType == null)
                throw invalidContainerException(container, null);

            Method m = annoType.members().get("value");
            if (m == null)
                throw invalidContainerException(container, null);

            m.setAccessible(true);

            // This will erase to (Annotation[]) but we do a runtime cast on the
            // return-value in the method that call this method.
            @SuppressWarnings("unchecked")
            A[] values = (A[]) m.invoke(container);

            return values;

        } catch (IllegalAccessException    | // couldn't loosen security
                 IllegalArgumentException  | // parameters doesn't match
                 InvocationTargetException | // the value method threw an exception
                 ClassCastException e) {

            throw invalidContainerException(container, e);

        }
    }


    private static AnnotationFormatError invalidContainerException(Annotation anno,
                                                                   Throwable cause) {
        return new AnnotationFormatError(
                anno + " is an invalid container for repeating annotations",
                cause);
    }


    /* Sanity check type of all the annotation instances of type {@code annoClass}
     * from {@code container}.
     */
    private static <A extends Annotation> void checkTypes(A[] annotations,
                                                          Annotation container,
                                                          Class<A> annoClass) {
        for (A a : annotations) {
            if (!annoClass.isInstance(a)) {
                throw new AnnotationFormatError(
                        String.format("%s is an invalid container for " +
                                      "repeating annotations of type: %s",
                                      container, annoClass));
            }
        }
    }
}
