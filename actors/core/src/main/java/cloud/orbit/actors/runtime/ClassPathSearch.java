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
package cloud.orbit.actors.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.ConcurrentHashSet;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Internal class to locate interface implementations.
 */
public class ClassPathSearch
{
    private static final Logger logger = LoggerFactory.getLogger(ClassPathSearch.class);
    private final ConcurrentHashSet<String> unprocessed = new ConcurrentHashSet<>();
    private final ConcurrentHashMap<String, ClassInfo> classes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Class<?>> concreteImplementations = new ConcurrentHashMap<>();
    private final ClassInfo[] classesOfInterestInfos;

    private static class ClassInfo
    {
        int modifiers = -1;
        Set<ClassInfo> allSuperClasses;
        Set<ClassInfo> allInterfaces;
        String name;

        public boolean isInterface()
        {
            return modifiers >= 0 && Modifier.isInterface(modifiers);
        }

        public int getModifiers()
        {
            return modifiers;
        }

        public boolean isAssignableFrom(ClassInfo clazz)
        {
            return clazz.isSubclassOf(this);
        }

        public boolean isSubclassOf(ClassInfo base)
        {
            if (this.equals(base) || allInterfaces.contains(base) || allSuperClasses.contains(base))
            {
                return true;
            }
            return false;
        }

        public Set<ClassInfo> getInterfaces()
        {
            return allInterfaces;
        }

        public ClassInfo(String name)
        {
            this.name = name;
        }

        @Override
        public boolean equals(Object o)
        {
            return (this == o) || (name.equals(((ClassInfo) o).name));
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }

    public ClassPathSearch(Class<?>... classesOfInterest)
    {
        this.classesOfInterestInfos = Stream.of(classesOfInterest)
                .map(c -> c.getName().replace('.', '/'))
                .map(this::getClassInfo)
                .toArray(ClassInfo[]::new);

        for (String rn : new FastClasspathScanner().scan().getNamesOfAllClasses())
        {
            classes.putIfAbsent(rn, new ClassInfo(rn));
        }

        unprocessed.addAll(classes.keySet());
    }

    public ClassInfo getClassInfo(String className)
    {
        // TODO: add default handling for posterior versions of the class file format

        ClassInfo info = classes.get(className);
        if (info != null && info.modifiers != -1)
        {
            return info;
        }
        final ClassInfo newInfo = new ClassInfo(className);
        newInfo.modifiers = 0;
        try
        {
            InputStream in = Actor.class.getResourceAsStream('/' + className.replace('.', '/') + ".class");
            if (in == null)
            {
                newInfo.allInterfaces = Collections.emptySet();
                newInfo.allSuperClasses = Collections.emptySet();
                classes.put(className, newInfo);
                return newInfo;
            }
            ClassReader reader = new ClassReader(in);

            int[] _modifiers = new int[1];
            String[] _superName = new String[1];
            String[][] _interfaces = new String[1][];

            reader.accept(new ClassVisitor(Opcodes.ASM4)
            {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
                {
                    _modifiers[0] = access;
                    _superName[0] = superName;
                    _interfaces[0] = interfaces;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);


            newInfo.modifiers = _modifiers[0];
            newInfo.allSuperClasses =
                    newInfo.allInterfaces = new HashSet<>(_interfaces[0].length);
            if (_superName[0] != null && !"java/lang/Object".equals(_superName[0]))
            {
                ClassInfo superClass = getClassInfo(_superName[0]);
                if (superClass != null)
                {
                    newInfo.allSuperClasses = new HashSet<>(superClass.allSuperClasses.size() + 1);
                    newInfo.allSuperClasses.add(superClass);
                    newInfo.allSuperClasses.addAll(superClass.allSuperClasses);
                    if (superClass.allInterfaces.size() > 0)
                    {
                        newInfo.allInterfaces.addAll(superClass.allInterfaces);
                    }
                }
            }
            for (String itf : _interfaces[0])
            {
                final ClassInfo itfInfo = getClassInfo(itf);
                newInfo.allInterfaces.add(itfInfo);
                newInfo.allInterfaces.addAll(itfInfo.allInterfaces);
            }

        }
        catch (IOException e)
        {
            // .. ignore
            newInfo.allInterfaces = newInfo.allInterfaces!=null ? newInfo.allInterfaces : Collections.emptySet();
            newInfo.allSuperClasses = newInfo.allSuperClasses!=null ? newInfo.allSuperClasses : Collections.emptySet();
        }
        classes.put(className, newInfo);
        return newInfo;
    }

    public <T, R extends T> Class<R> findImplementation(Class<T> theInterface)
    {
        return findImplementation(theInterface, classes.size());
    }

    @SuppressWarnings("unchecked")
    public <T, R extends T> Class<R> findImplementation(Class<T> theInterface, int limit)
    {
        Class<?> implementationClass = concreteImplementations.get(theInterface);
        if (implementationClass != null)
        {
            return (Class<R>) implementationClass;
        }
        return doFind(theInterface, limit);
    }

    private synchronized <T, R extends T> Class<R> doFind(final Class<T> theInterface, final int limit)
    {
        final Class<?> implementationClass;
        if (unprocessed.size() == 0)
        {
            return null;
        }

        final String expectedName = theInterface.getName().replace('.', '/');
        final ClassInfo theInterfaceInfo = getClassInfo(expectedName);
        // cloning the list of unprocessed since it might be modified by the operations on the stream.

        if (logger.isDebugEnabled())
        {
            logger.debug("Searching implementation class for: " + theInterface);
        }
        // searching
        implementationClass = Stream.of(unprocessed.toArray())
                .map(o -> (String) o)
                .sorted((a, b) -> {
                    // order by closest name to the interface.
                    int sa = commonStart(expectedName, a) + commonEnd(expectedName, a);
                    int sb = commonStart(expectedName, b) + commonEnd(expectedName, b);
                    return (sa != sb) ? (sb - sa) : a.length() - b.length();
                })
                .limit(limit)
                        // fixes a bug with older java 8 versions
                .collect(Collectors.toList()).stream()
                        // use this for development: .peek(System.out::println)
                .map(cn -> {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Checking: " + cn);
                    }
                    // this returns non null if there is a match
                    // it also culls the list
                    try
                    {
                        ClassInfo clazz = getClassInfo(cn);

                        if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                        {
                            if (theInterfaceInfo.isAssignableFrom(clazz))
                            {
                                // when searching for Hello1, must avoid:
                                // Hello1 <- HelloActor2
                                // Hello1 <- HelloImpl1
                                // Hello2 <-s HelloImpl2  (wrong return, least strict).

                                // However the application **should not** do this kind of class tree.

                                // Important: this makes ClassPathSearch non generic.
                                for (ClassInfo i : clazz.getInterfaces())
                                {
                                    if (i != theInterfaceInfo && theInterfaceInfo.isAssignableFrom(i))
                                    {
                                        return null;
                                    }
                                }
                                // found the best match!
                                return Class.forName(cn.replace('/', '.'));
                            }
                        }
                        if (classesOfInterestInfos.length > 0)
                        {
                            for (ClassInfo base : classesOfInterestInfos)
                            {
                                if (base.isAssignableFrom(clazz))
                                {
                                    // keep the classes that are part of the classes of interest list
                                    return null;
                                }
                            }
                            unprocessed.remove(cn);
                        }
                        return null;
                    }
                    catch (Throwable e)
                    {
                        // there is some problem with this class
                        // culling the list for the next search
                        logger.error("Error finding implementation of: "
                                + theInterface.getName() + " as " + cn, e);
                        unprocessed.remove(cn);
                        return null;
                    }
                })
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);

        if (implementationClass != null)
        {
            // cache the findings.
            concreteImplementations.put(theInterface, implementationClass);
        }
        return (Class<R>) implementationClass;
    }


    /**
     * Returns the size of the common start the two strings
     * <p/>
     * Example:
     * <pre>commonStart("ssssBBB", "ssCCC") == 2</pre>
     */
    static int commonStart(String a, String b)
    {
        int c = 0, len = Math.min(a.length(), b.length());
        for (; c < len && a.charAt(c) == b.charAt(c); )
        {
            c++;
        }
        return c;
    }

    /**
     * Returns the size of the common end the two strings
     * <p/>
     * Example:
     * <pre>commonEnd("AAAAeeee", "BBBee") == 2</pre>
     */
    static int commonEnd(String a, String b)
    {
        int c = 0;
        int ia = a.length();
        int ib = b.length();

        for (; --ia >= 0 && --ib >= 0 && a.charAt(ia) == b.charAt(ib); )
        {
            c++;
        }
        return c;
    }

}
