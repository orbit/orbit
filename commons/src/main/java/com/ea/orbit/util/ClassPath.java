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

import com.ea.orbit.exception.UncheckedException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassPath
{
    public static final String CLASS_FILE_EXTENSION = ".class";
    private static Logger logger = Logger.getLogger(ClassPath.class.getName());
    private static volatile ClassPath sharedInstance;
    private static final Object MUTEX = new Object();

    private List<ResourceInfo> resources;
    private List<ClassResourceInfo> classes;

    public static class ResourceInfo
    {
        protected String resourceName;
        protected ClassLoader loader;

        private ResourceInfo(final String resourceName, final ClassLoader loader)
        {
            this.resourceName = (resourceName);
            this.loader = (loader);
        }

        public URL url()
        {
            return loader.getResource(resourceName);
        }

        public String getResourceName()
        {
            return resourceName;
        }

    }

    public static class ClassResourceInfo extends ResourceInfo
    {
        private String className;

        private ClassResourceInfo(final String resourceName, final ClassLoader loader)
        {
            super(resourceName, loader);

        }

        public Class<?> load() throws ClassNotFoundException
        {
            return loader.loadClass(className);
        }

        public String getClassName()
        {
            return className != null ? className
                    : (className = resourceName
                    .substring(0, resourceName.length() - CLASS_FILE_EXTENSION.length())
                    .replace('/', '.').intern());
        }
    }

    public List<ResourceInfo> getAllResources()
    {
        return resources;
    }

    public List<ClassResourceInfo> getAllClasses()
    {
        return classes;
    }

    public static ClassPath from(final ClassLoader classloader) throws IOException
    {
        final List<ResourceInfo> resourceInfos = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final Queue<URL> urlsQueue = new LinkedList<>();
        for (ClassLoader c = classloader; c != null; c = c.getParent())
        {
            final ClassLoader loader = c;
            if (!(c instanceof URLClassLoader))
            {
                continue;
            }
            final URL[] urls = ((URLClassLoader) c).getURLs();
            if (urls == null || urls.length == 0)
            {
                continue;
            }
            urlsQueue.addAll(Arrays.asList(urls));
            while (urlsQueue.size() > 0)
            {
                final URL url = urlsQueue.remove();
                if (!seen.add(url.toString()))
                {
                    continue;
                }
                if (!"file".equals(url.getProtocol()))
                {
                    if (logger.getLevel() == Level.INFO)
                    {
                        logger.info("Ignoring classpath entry: " + url);
                    }
                    continue;
                }
                try
                {
                    final Path entryPath = Paths.get(url.toURI());
                    if (Files.isDirectory(entryPath))
                    {
                        resourceInfos.addAll(Files.walk(entryPath)
                                .filter(x -> !Files.isDirectory(x))
                                .map(x -> createResourceInfo(entryPath.relativize(x).toString().replace(File.separatorChar, '/'), loader))
                                .collect(Collectors.toList()));
                    }
                    else
                    {
                        JarFile jar = null;
                        try
                        {
                            jar = new JarFile(entryPath.toFile());

                            resourceInfos.addAll(Collections.list(jar.entries()).stream()
                                    .filter(x -> !x.isDirectory())
                                    .map(x -> createResourceInfo(x.getName(), loader))
                                    .collect(Collectors.toList()));

                            // getting the classpath from the jar manifest
                            // obs.: the maven surefire plugin uses the manifest attribute.
                            final Manifest manifest = jar.getManifest();
                            final Attributes mainAttributes = manifest == null ? null : manifest.getMainAttributes();
                            final String classpath = mainAttributes == null ? null : mainAttributes.getValue(Attributes.Name.CLASS_PATH.toString());
                            if (classpath != null)
                            {
                                for (final String path : classpath.split(" "))
                                {
                                    try
                                    {
                                        URI uri = new URI(path);
                                        urlsQueue.add((uri.isAbsolute() ? uri : new File(entryPath.toFile(), path).toURI()).toURL());
                                    }
                                    catch (final URISyntaxException e)
                                    {
                                        logger.warning("Ignoring class path element: " + path + " from: " + entryPath);
                                    }
                                }
                            }
                        }
                        catch (IOException ex)
                        {
                            if (logger.isLoggable(Level.FINE))
                            {
                                logger.log(Level.FINE, "Ignoring unsupported or malformed jar resource: " + entryPath);
                            }
                        }
                        finally
                        {
                            IOUtils.silentlyClose(jar);
                        }
                    }
                }
                catch (URISyntaxException ex)
                {
                    throw new UncheckedException("Error loading classpath resources from: " + url, ex);
                }
            }

        }
        ClassPath cp = new ClassPath();
        cp.resources = resourceInfos;
        cp.classes = resourceInfos.stream()
                .filter(r -> r instanceof ClassResourceInfo)
                .map(r -> (ClassResourceInfo) r)
                .collect(Collectors.toList());
        return cp;
    }

    private static ResourceInfo createResourceInfo(final String name, final ClassLoader loader)
    {
        return name.endsWith(CLASS_FILE_EXTENSION)
                ? new ClassResourceInfo(name, loader)
                : new ResourceInfo(name, loader);
    }

    public static ClassPath get()
    {
        ClassPath res = sharedInstance;
        if (res != null)
        {
            return res;
        }
        synchronized (MUTEX)
        {
            // it's better to synchronize to avoid iterating over the classpath multiple times.
            // double checked locking has it's pitfalls... check the volatile attribute.
            // http://en.wikipedia.org/wiki/Double-checked_locking
            res = sharedInstance;
            if (res == null)
            {
                try
                {
                    sharedInstance = res = from(ClassPath.class.getClassLoader());
                }
                catch (final IOException e)
                {
                    throw new UncheckedException(e);
                }
            }
        }
        return res;
    }

    public static String getNullSafePackageName(final Class<?> clazz)
    {
        return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
    }

}
