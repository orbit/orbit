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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.annotation.ClassIdStrategy;
import com.ea.orbit.actors.reflection.ClassIdGenerationStrategy;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ClassPath;
import com.ea.orbit.util.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class DefaultClassDictionary
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultClassDictionary.class);
    public static final String META_INF_SERVICES_ORBIT_CLASSES = "META-INF/services/orbit/classes/";
    public static final String SUFFIX = ".yml";
    public static final Object LOAD_MUTEX = new Object();
    private static DefaultClassDictionary instance = new DefaultClassDictionary();
    private ConcurrentMap<Class<?>, Integer> classToId = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, Class<?>> idToClass = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, String> idToName = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Integer> nameToId = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    private DefaultClassDictionary()
    {

    }

    public static DefaultClassDictionary get()
    {
        return instance;
    }

    /**
     * Loads classIds written to classpath with the annotation processor
     * to @code{META-INF/services/orbit/classes/binary-class-name.yml]
     * <p>File format (yaml):
     * <pre>
     * classId: integer
     * </pre>
     * </p>
     */
    private void ensureLoaded()
    {
        if (loaded)
        {
            return;
        }

        synchronized (LOAD_MUTEX)
        {
            if (loaded)
            {
                return;
            }
            ClassPath.get().getAllResources().stream()
                    .filter(r -> r.getResourceName().startsWith(META_INF_SERVICES_ORBIT_CLASSES) && r.getResourceName().endsWith(SUFFIX))
                    .forEach(r -> loadClassInfo(r));
            loaded = true;
        }
    }

    private void loadClassInfo(final ClassPath.ResourceInfo r)
    {
        final URL url = r.url();
        try
        {
            final LinkedHashMap map = new Yaml().loadAs(url.openStream(), LinkedHashMap.class);
            final Object classId = map.get("classId");
            if (classId instanceof Number)
            {
                final String resourceName = r.getResourceName();
                final Integer classIdKey = ((Number) classId).intValue();
                final String className = resourceName.substring(META_INF_SERVICES_ORBIT_CLASSES.length(),
                        resourceName.length() - SUFFIX.length());
                idToName.putIfAbsent(classIdKey, className);
                nameToId.putIfAbsent(className, classIdKey);
            }
            else
            {
                logger.warn("classId not found at: " + url);
            }
        }
        catch (Exception ex)
        {
            logger.error("Error loading class info " + url, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(final String className, boolean ignoreException)
    {
        try
        {
            return (Class<T>) Class.forName(className);
        }
        catch (Error | Exception ex)
        {
            if (!ignoreException)
            {
                throw new Error("Error loading class: " + className, ex);
            }
        }
        return null;
    }

    public Class<?> getClassById(int classId)
    {
        final Integer id = classId;
        Class<?> clazz = idToClass.get(id);
        if (clazz != null)
        {
            return clazz;
        }


        String className = idToName.get(id);
        if (className == null)
        {
            // reading the class name saved in the file system by the annotation processor
            String fileName = "/META-INF/services/orbit/classId/"
                    + (Math.abs(classId) % 10)
                    + "/" + (Math.abs(classId / 10) % 10)
                    + ((classId < 0) ? "/m" : "/") + Math.abs(classId) + ".name";

            InputStream resourceAsStream = getClass().getResourceAsStream(fileName);
            if (resourceAsStream != null)
            {
                try
                {
                    className = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
                }
                catch (IOException e)
                {
                    logger.error("Error loading resource: " + fileName);
                }
            }
        }

        ensureLoaded();

        if (className == null)
        {
            final List<ClassPath.ClassResourceInfo> list = ClassPath.get().getAllClasses().stream()
                    .filter(c -> c.getClassName().hashCode() == classId)
                    .collect(Collectors.toList());
            if (list.size() > 0)
            {
                className = list.get(0).getClassName();
            }
            if (list.size() == 0)
            {
                throw new UncheckedException("Class not found classId:" + id);
            }
        }
        clazz = classForName(className, false);
        int actualClassId = getClassId(clazz);
        // double check if the application is not using a hash based id when the correct one is stored somewhere else
        if (actualClassId != classId)
        {
            logger.error("Using invalid class id for {} {}!={}, should be {}", className, classId, actualClassId, actualClassId);
            throw new IllegalArgumentException(String.format("Using invalid class id for %s %d!=%d, should be %d", className, classId, actualClassId, actualClassId));
        }
        classToId.putIfAbsent(clazz, id);
        idToClass.putIfAbsent(id, clazz);
        return clazz;
    }

    public int getClassId(Class<?> clazz)
    {
        Integer id = classToId.get(clazz);
        if (id != null)
        {
            return id;
        }
        //ensureLoaded();
        final String className = clazz.getName();

        id = nameToId.get(className);
        if (id == null)
        {
            for (Annotation ann : clazz.getAnnotations())
            {
                ClassIdStrategy idStrategy = ann.annotationType().getAnnotation(ClassIdStrategy.class);
                if (idStrategy != null)
                {
                    ClassIdGenerationStrategy idGenerationStrategy;
                    try
                    {
                        idGenerationStrategy = idStrategy.value().newInstance();
                        id = idGenerationStrategy.generateIdForClass(ann, className);
                        if (id != 0)
                        {
                            break;
                        }
                    }
                    catch (Exception ex)
                    {
                        throw new UncheckedException("Error generating id for " + className, ex);
                    }
                }
            }
            if (id == null || id == 0)
            {
                // fall back to hashCode
                id = className.hashCode();
            }
            nameToId.putIfAbsent(className, id);
            idToName.putIfAbsent(id, className);
        }
        classToId.putIfAbsent(clazz, id);
        idToClass.putIfAbsent(id, clazz);
        return id;
    }
}

