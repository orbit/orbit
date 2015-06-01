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

package com.ea.orbit.instrumentation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Class path and class loader utilities to help java agent developers.
 *
 * @author Daniel Sperry
 */
public class ClassPathUtils
{

    /**
     * Returns the path for the jar or directory that contains a given java class.
     *
     * @param clazz the class whose classpath entry needs to be located.
     * @return an url with the jar or directory that contains that class
     */
    public static URL getClassPathFor(final Class<?> clazz)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("Null class");
        }
        try
        {
            final URL url = getClassFile(clazz);
            String urlString = url.toString();
            final int idx = urlString.toLowerCase().indexOf(".jar!");
            if (idx > 0)
            {
                final int idx2 = urlString.lastIndexOf("file:/");
                return new URL(urlString.substring(idx2 >= 0 ? idx2 : 0, idx + 4));
            }
            else
            {
                File dir = new File(url.toURI()).getParentFile();
                if (clazz.getPackage() != null)
                {
                    String pn = clazz.getPackage().getName();
                    for (int i = pn.indexOf('.'); i >= 0; i = pn.indexOf('.', i + 1))
                    {
                        dir = dir.getParentFile();
                    }
                    dir = dir.getParentFile();
                }
                return dir.toURI().toURL();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error locating classpath entry for: " + clazz.getName(), e);
        }
    }

    public static String getNullSafePackageName(final Class<?> clazz)
    {
        return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
    }

    /**
     * Get the actual classpath resource location for a class
     *
     * @param clazz the class whose .class file must be located.
     * @return a resource url or null if the class is runtime generated.
     */
    public static URL getClassFile(final Class<?> clazz)
    {
        int idx = clazz.getName().lastIndexOf('.');
        final String fileName = (idx >= 0 ? clazz.getName().substring(idx + 1) : clazz.getName()) + ".class";
        return clazz.getResource(fileName);
    }


    /**
     * Appends a directory or jar to the system class loader.
     *
     * Do this only if you understand the consequences:
     * <ul>
     * <li>classes loaded by different class loaders are different classes</li>
     * <li>if the application uses the same classes the load order might create inconsistencies</li>
     * </ul>
     *
     * @param path the url to append to the system class loader
     */
    public static void appendToSystemPath(URL path)
    {
        if (path == null)
        {
            throw new IllegalArgumentException("Null path");
        }
        try
        {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{ URL.class });
            method.setAccessible(true);
            method.invoke(systemClassLoader, path);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Add URL failed: " + path, ex);
        }
    }

    static byte[] toByteArray(InputStream input) throws IOException
    {
        byte[] buffer = new byte[Math.max(1024, input.available())];
        int offset = 0;
        for (int bytesRead; -1 != (bytesRead = input.read(buffer, offset, buffer.length - offset)); )
        {
            offset += bytesRead;
            if (offset == buffer.length)
            {
                buffer = Arrays.copyOf(buffer, buffer.length + Math.max(input.available(), buffer.length >> 1));
            }
        }
        return (offset == buffer.length) ? buffer : Arrays.copyOf(buffer, offset);
    }

    public static Class<?> defineClass(ClassLoader loader, InputStream inputStream)
    {
        try
        {
            final byte[] bytes = toByteArray(inputStream);
            final Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClassMethod.setAccessible(true);
            return (Class<?>) defineClassMethod.invoke(loader, null, bytes, 0, bytes.length);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
