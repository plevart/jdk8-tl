/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

import sun.misc.JavaLangAccess;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Represents an annotation type at run time.  Used to type-check annotations
 * and apply member defaults.
 *
 * @author  Josh Bloch
 * @since   1.5
 */
public class AnnotationType {
    /**
     * for accessing package-private Class.getDirectDeclaredAnnotation & Class.[get|set]AnnotationType
     */
    private static final JavaLangAccess JAVA_LANG_ACCESS = sun.misc.SharedSecrets.getJavaLangAccess();

    /**
     * The in-construction instance that can be obtained half-constructed from recursive calls
     */
    private static final ClassValue<ThreadLocal<AnnotationType>> IN_CONSTRUCTION = new ClassValue<ThreadLocal<AnnotationType>>() {
        @Override
        protected ThreadLocal<AnnotationType> computeValue(Class<?> type) {
            return new ThreadLocal<>();
        }
    };

    /**
     * Member name -> type mapping. Note that primitive types
     * are represented by the class objects for the corresponding wrapper
     * types.  This matches the return value that must be used for a
     * dynamic proxy, allowing for a simple isInstance test.
     */
    private final Map<String, Class<?>> memberTypes = new HashMap<String,Class<?>>();

    /**
     * Member name -> default value mapping.
     */
    private final Map<String, Object> memberDefaults =
        new HashMap<String, Object>();

    /**
     * Member name -> Method object mapping. This (and its assoicated
     * accessor) are used only to generate AnnotationTypeMismatchExceptions.
     */
    private final Map<String, Method> members = new HashMap<String, Method>();

    /**
     * The retention policy for this annotation type.
     */
    private final RetentionPolicy retention;

    /**
     * Whether this annotation type is inherited.
     */
    private final boolean inherited;

    /**
     * Associated container and containee annotation classes for repeating annotations resolution
     */
    private final Class<? extends Annotation> container;
    private final Class<? extends Annotation> containee;

    /**
     * Returns an AnnotationType instance for the specified annotation type.
     *
     * @throw IllegalArgumentException if the specified class object for
     *     does not represent a valid annotation type
     */
    public static AnnotationType getInstance(
        Class<? extends Annotation> annotationClass)
    {
        AnnotationType result = JAVA_LANG_ACCESS.getAnnotationType(annotationClass);
        if (result == null) {
            if (!annotationClass.isAnnotation())
                throw new IllegalArgumentException("Not an annotation type");
            // check to see if this is a recursive call from the constructor
            result = IN_CONSTRUCTION.get(annotationClass).get();
            if (result == null) {
                result = new AnnotationType(annotationClass);
                // install into annotationClass
                JAVA_LANG_ACCESS.setAnnotationType(annotationClass, result);
                // remove the ThreadLocal value
                IN_CONSTRUCTION.get(annotationClass).remove();
            }
        }

        return result;
    }

    /**
     * Sole constructor.
     *
     * @param annotationClass the class object for the annotation type
     * @throw IllegalArgumentException if the specified class object for
     *     does not represent a valid annotation type
     */
    private AnnotationType(final Class<? extends Annotation> annotationClass) {

        Method[] methods =
            AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
                public Method[] run() {
                    // Initialize memberTypes and defaultValues
                    return annotationClass.getDeclaredMethods();
                }
            });

        for (Method method :  methods) {
            if (method.getParameterTypes().length != 0)
                throw new IllegalArgumentException(method + " has params");
            String name = method.getName();
            Class<?> type = method.getReturnType();
            memberTypes.put(name, invocationHandlerReturnType(type));
            members.put(name, method);

            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null)
                memberDefaults.put(name, defaultValue);
        }

        // Initialize retention, inherited, container & containee fields.  Special treatment
        // of the corresponding annotation types breaks infinite recursion.
        if (annotationClass != Retention.class &&
            annotationClass != Inherited.class &&
            annotationClass != ContainedBy.class &&
            annotationClass != ContainerFor.class) {
            // make available to constructing thread a half-initialized instance
            IN_CONSTRUCTION.get(annotationClass).set(this);
            // following calls can be recursive
            Retention ret = JAVA_LANG_ACCESS.getDirectDeclaredAnnotation(annotationClass, Retention.class);
            retention = ret == null ? RetentionPolicy.CLASS : ret.value();
            inherited = JAVA_LANG_ACCESS.getDirectDeclaredAnnotation(annotationClass, Inherited.class) != null;
            ContainedBy containedBy = JAVA_LANG_ACCESS.getDirectDeclaredAnnotation(annotationClass, ContainedBy.class);
            container = containedBy == null ? Annotation.class : containedBy.value();
            ContainerFor containerFor = JAVA_LANG_ACCESS.getDirectDeclaredAnnotation(annotationClass, ContainerFor.class);
            containee = containerFor == null ? Annotation.class : containerFor.value();
        }
        else
        {
            retention = RetentionPolicy.RUNTIME;
            inherited = false;
            container = Annotation.class;
            containee = Annotation.class;
        }
    }

    /**
     * Returns the type that must be returned by the invocation handler
     * of a dynamic proxy in order to have the dynamic proxy return
     * the specified type (which is assumed to be a legal member type
     * for an annotation).
     */
    public static Class<?> invocationHandlerReturnType(Class<?> type) {
        // Translate primitives to wrappers
        if (type == byte.class)
            return Byte.class;
        if (type == char.class)
            return Character.class;
        if (type == double.class)
            return Double.class;
        if (type == float.class)
            return Float.class;
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == short.class)
            return Short.class;
        if (type == boolean.class)
            return Boolean.class;

        // Otherwise, just return declared type
        return type;
    }

    /**
     * Returns member types for this annotation type
     * (member name -> type mapping).
     */
    public Map<String, Class<?>> memberTypes() {
        return memberTypes;
    }

    /**
     * Returns members of this annotation type
     * (member name -> associated Method object mapping).
     */
    public Map<String, Method> members() {
        return members;
    }

    /**
     * Returns the default values for this annotation type
     * (Member name -> default value mapping).
     */
    public Map<String, Object> memberDefaults() {
        return memberDefaults;
    }

    /**
     * Returns the retention policy for this annotation type.
     */
    public RetentionPolicy retention() {
        // default when called recursively into a half-initialized instance is RetentionPolicy.RUNTIME
        return retention == null ? RetentionPolicy.RUNTIME : retention;
    }

    /**
     * Returns true if this annotation type is inherited.
     */
    public boolean isInherited() {
        // default when called recursively into a half-initialized instance is false
        return inherited;
    }

    /**
     * Returns the container annotation class for this annotation type if any or null
     */
    public Class<? extends Annotation> getContainer() {
        // should not need to call recursively into a half-initialized instance
        if (container == null) throw new IllegalStateException("Trying to obtain container while not initialized yet");
        return container == Annotation.class ? null : container;
    }

    /**
     * Returns the containee annotation class for this annotation type if any or null
     */
    public Class<? extends Annotation> getContainee() {
        // should not need to call recursively into a half-initialized instance
        if (containee == null) throw new IllegalStateException("Trying to obtain containee while not initialized yet");
        return containee == Annotation.class ? null : containee;
    }

    /**
     * For debugging.
     */
    public String toString() {
        StringBuffer s = new StringBuffer("Annotation Type:" + "\n");
        s.append("   Member types: " + memberTypes + "\n");
        s.append("   Member defaults: " + memberDefaults + "\n");
        s.append("   Retention policy: " + retention() + "\n");
        s.append("   Inherited: " + inherited + "\n");
        s.append("   Container: " + getContainer() + "\n");
        s.append("   Containee: " + getContainee() );
        return s.toString();
    }
}
