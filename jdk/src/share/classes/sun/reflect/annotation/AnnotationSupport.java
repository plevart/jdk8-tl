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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sun.misc.JavaLangAccess;

public final class AnnotationSupport {
    private static final JavaLangAccess LANG_ACCESS = sun.misc.SharedSecrets.getJavaLangAccess();

    /**
     * Finds and returns all annotations in {@code annotations} matching
     * the given {@code annoClass}.
     *
     * Apart from annotations directly present in {@code annotations} this
     * method searches for annotations inside containers i.e. indirectly
     * present annotations.
     *
     * The order of the elements in the array returned depends on the iteration
     * order of the provided map. Specifically, the directly present annotations
     * come before the indirectly present annotations if and only if the
     * directly present annotations come before the indirectly present
     * annotations in the map.
     *
     * @param annotations the {@code Map} in which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty
     *         array if none were found
     */
    public static <A extends Annotation> A[] getDirectlyAndIndirectlyPresent(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {
        @SuppressWarnings("unchecked")
        A direct = (A) annotations.get(annoClass);
        A[] indirect = getSharedIndirectlyPresent(annotations, annoClass);
        A[] result;

        if (direct == null) {
            if (indirect == null) {
                result = newArray(annoClass, 0);
            } else {
                result = cloneArray(indirect, annoClass);
            }
        } else {
            if (indirect == null || indirect.length == 0) {
                result = newArray(annoClass, 1);
                result[0] = direct;
            } else {
                result = newArray(annoClass, 1 + indirect.length);
                if (containerBeforeContainee(annotations, annoClass)) {
                    System.arraycopy(indirect, 0, result, 0, indirect.length);
                    result[indirect.length] = direct;
                } else {
                    result[0] = direct;
                    System.arraycopy(indirect, 0, result, 1, indirect.length);
                }
            }
        }

        return result;
    }

    /**
     * Finds and returns all annotations matching the given {@code annoClass}
     * indirectly present in {@code annotations}.
     *
     * @param annotations annotations to search indexed by their types
     * @param annoClass the type of annotation to search for
     *
     * @return a shared array of instances of {@code annoClass} or null
     *         (or empty array) if no indirectly present annotations were found
     */
    private static <A extends Annotation> A[] getSharedIndirectlyPresent(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {

        Repeatable repeatable = annoClass.getDeclaredAnnotation(Repeatable.class);
        if (repeatable == null)
            return null;  // Not repeatable -> no indirectly present annotations

        Class<? extends Annotation> containerClass = repeatable.value();

        Annotation container = annotations.get(containerClass);
        if (container == null)
            return null;

        return getSharedValueArray(container, annoClass);
    }


    /**
     * Figures out if conatiner class comes before containee class among the
     * keys of the given map.
     *
     * @return true if container class is found before containee class when
     *         iterating over annotations.keySet().
     */
    private static <A extends Annotation> boolean containerBeforeContainee(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {

        Class<? extends Annotation> containerClass =
                annoClass.getDeclaredAnnotation(Repeatable.class).value();

        for (Class<? extends Annotation> c : annotations.keySet()) {
            if (c == containerClass) return true;
            if (c == annoClass) return false;
        }

        // Neither containee nor container present
        return false;
    }


    /**
     * Finds and returns all associated annotations matching the given class.
     *
     * The order of the elements in the array returned depends on the iteration
     * order of the provided maps. Specifically, the directly present annotations
     * come before the indirectly present annotations if and only if the
     * directly present annotations come before the indirectly present
     * annotations in the relevant map.
     *
     * @param declaredAnnotations the declared annotations indexed by their types
     * @param decl the class declaration on which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty array if none were found.
     */
    public static <A extends Annotation> A[] getAssociatedAnnotations(
            Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
            Class<?> decl,
            Class<A> annoClass) {
        Objects.requireNonNull(decl);

        // Search declared
        A[] result = getDirectlyAndIndirectlyPresent(declaredAnnotations, annoClass);

        // Search inherited
        if(AnnotationType.getInstance(annoClass).isInherited()) {
            Class<?> superDecl = decl.getSuperclass();
            while (result.length == 0 && superDecl != null) {
                result = getDirectlyAndIndirectlyPresent(LANG_ACCESS.getDeclaredAnnotationMap(superDecl), annoClass);
                superDecl = superDecl.getSuperclass();
            }
        }

        return result;
    }

    private static <A extends Annotation> A[] getSharedValueArray(Annotation container, Class<A> annoClass) {
        try {
            @SuppressWarnings("unchecked")
            A[] array = (A[]) AnnotationInvocationHandler.getMemberValue(container, "value");
            if (array == null)
                throw invalidContainerException(container, null);
            checkTypes(array, container, annoClass);
            return array;
        } catch (ClassCastException e) {
            throw invalidContainerException(container, e);
        }

    }

    private static <A extends Annotation> A[] getValueArray(Annotation container, Class<A> annoClass) {
        return cloneArray(getSharedValueArray(container, annoClass), annoClass);
    }

    private static  <A extends Annotation> A[] cloneArray(A[] array, Class<A> annoClass) {
        A[] copiedArray = newArray(annoClass, array.length);
        System.arraycopy(array, 0, copiedArray, 0, array.length);
        return copiedArray;
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> A[] newArray(Class<A> annoClass, int length) {
        return (A[]) Array.newInstance(annoClass, length);
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
