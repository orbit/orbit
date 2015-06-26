/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.util;

import com.ea.orbit.concurrent.ConcurrentHashSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public class AnnotationUtils
{

    private static Map<Class<?>, Annotation> clazzAnnotationCache = new ConcurrentHashMap<>();
    private static Set<Class<?>> clazzAnnotationMissCache = new ConcurrentHashSet<>();

    private static Map<Method, Annotation> methodAnnotationCache = new ConcurrentHashMap<>();
    private static Set<Method> methodAnnotationMissCache = new ConcurrentHashSet<>();

    private AnnotationUtils()
    {
        // Placating HideUtilityClassConstructorCheck
    }

    /**
     * Return given annotation if present on a class (including inherited annotations).
     *
     * @param clazz           the class.
     * @param annotationClass the annotation.
     * @return the annotation or null if not present.
     */
    public static Annotation getAnnotation(final Class<?> clazz, final Class<? extends Annotation> annotationClass)
    {
        if (clazz == null || annotationClass == null)
        {
            return null;
        }

        if (clazzAnnotationMissCache.contains(clazz))
        {
            return null;
        }

        Annotation annotation = clazzAnnotationCache.get(clazz);

        if (annotation != null)
        {
            return annotation;
        }

        for (final Class<?> c : getClasses(clazz))
        {
            annotation = c.getAnnotation(annotationClass);
            if (annotation != null)
            {
                break; // return first declared annotation
            }
        }

        if (annotation != null)
        {
            clazzAnnotationCache.put(clazz, annotation);
        }
        else
        {
            clazzAnnotationMissCache.add(clazz);
        }

        return annotation;
    }

    /**
     * Return given annotation if present on a method (including inherited annotations).
     *
     * @param method          the method.
     * @param annotationClass the annotation.
     * @return the annotation or null if not present.
     */
    public static Annotation getAnnotation(final Method method, final Class<? extends Annotation> annotationClass)
    {
        if (method == null || annotationClass == null)
        {
            return null;
        }

        if (methodAnnotationMissCache.contains(method))
        {
            return null;
        }

        Annotation annotation = methodAnnotationCache.get(method);

        if (annotation != null)
        {
            return annotation;
        }

        for (final Class<?> clazz : getClasses(method.getDeclaringClass()))
        {
            try
            {
                annotation = clazz.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(annotationClass);
                if (annotation != null)
                {
                    break; // return first declared annotation
                }
            }
            catch (final NoSuchMethodException ignore)
            {
            }
        }

        if (annotation != null)
        {
            methodAnnotationCache.put(method, annotation);
        }
        else
        {
            methodAnnotationMissCache.add(method);
        }

        return annotation;
    }

    /**
     * Check whether a class is annotated with a specific annotation.
     *
     * @param clazz           the class.
     * @param annotationClass the annotation.
     * @return if the annotation is present.
     */
    public static boolean isAnnotationPresent(final Class<?> clazz, final Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(clazz, annotationClass) != null;
    }

    /**
     * Check whether a method is annotated with a specific annotation.
     *
     * @param method     the method.
     * @param annotation the annotation.
     * @return if the annotation is present.
     */
    public static boolean isAnnotationPresent(final Method method, final Class<? extends Annotation> annotation)
    {
        return getAnnotation(method, annotation) != null;
    }

    /**
     * Returns extended classes and interfaces implemented by the given class (including itself).
     *
     * @param clazz the class.
     * @return all extended classes and interfaces.
     */
    private static Collection<Class<?>> getClasses(Class<?> clazz)
    {
        final Collection<Class<?>> classes = new HashSet<>();
        while (clazz != null)
        {
            classes.add(clazz);
            Collections.addAll(classes, clazz.getInterfaces());

            clazz = clazz.getSuperclass();
        }
        return classes;
    }
}
