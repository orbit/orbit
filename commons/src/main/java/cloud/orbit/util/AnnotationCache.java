/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class which provides cache for expensive annotation lookups.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class AnnotationCache<T extends Annotation>
{
    private final Annotation nullAnnotation = new NullAnnotation() {
        @Override
        public Class<? extends Annotation> annotationType()
        {
            return NullAnnotation.class;
        }
    };

    private final Class<T> annotationClazz;

    private ConcurrentMap<Method, Annotation> methodToAnnotationCache = new ConcurrentHashMap<>();

    public AnnotationCache(final Class<T> annotationClazz)
    {
        this.annotationClazz = annotationClazz;
    }

    public boolean isAnnotated(Method method)
    {
        if (method == null) {
            return false;
        }
        Annotation annotation = methodToAnnotationCache.get(method);
        if (annotation != null)
        {
            return annotation != nullAnnotation;
        }
        annotation = method.getAnnotation(annotationClazz);
        methodToAnnotationCache.putIfAbsent(method, annotation != null ? annotation : nullAnnotation);
        return annotation != null;
    }

    @SuppressWarnings("unchecked")
    public T getAnnotation(Method method)
    {
        if (method == null) {
            return null;
        }
        Annotation annotation = methodToAnnotationCache.get(method);
        if (annotation != null)
        {
            return annotation != nullAnnotation ? (T) annotation : null;
        }
        annotation = method.getAnnotation(annotationClazz);
        methodToAnnotationCache.putIfAbsent(method, annotation != null ? annotation : nullAnnotation);
        return (T) annotation;
    }

    private @interface NullAnnotation {
    }
}
