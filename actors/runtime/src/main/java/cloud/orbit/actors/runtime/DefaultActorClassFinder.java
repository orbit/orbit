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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.extensions.ActorClassFinder;
import cloud.orbit.concurrent.Task;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class DefaultActorClassFinder implements ActorClassFinder
{
    private static Logger logger = LoggerFactory.getLogger(DefaultActorClassFinder.class);

    private final String[] scanSpec;
    private final Map<Class<?>, Class<?>> concreteImplementations = new ConcurrentHashMap<>();

    public DefaultActorClassFinder(final String... actorBasePackages)
    {
        this.scanSpec = extractScanSpec(actorBasePackages);
    }

    @Override
    public Task<?> start()
    {
        List<Class<?>> clazzImplementations = new ArrayList<>();
        long start = System.currentTimeMillis();
        FastClasspathScanner scanner = new FastClasspathScanner(scanSpec).matchClassesImplementing(Actor.class, candidate -> {
            if (candidate.getSimpleName().toLowerCase(Locale.ENGLISH).startsWith("abstract"))
            {
                return;
            }
            clazzImplementations.add(candidate);
        });
        if (logger.isTraceEnabled())
        {
            scanner.verbose();
        }
        scanner.scan();
        for (Class<?> clazzImplementation : clazzImplementations)
        {
            Class<?>[] implementationInterfaces = clazzImplementation.getInterfaces();
            if (implementationInterfaces.length == 0)
            {
                continue;
            }
            for (Class<?> implementationInterface : implementationInterfaces)
            {
                if (implementationInterface == Remindable.class)
                {
                    continue;
                }
                if (Actor.class.isAssignableFrom(implementationInterface))
                {
                    Class<?> old = concreteImplementations.put(implementationInterface, clazzImplementation);
                    if (old != null)
                    {
                        logger.warn("Multiple actor implementations found for " + implementationInterface);
                    }
                }
            }
        }
        long end = System.currentTimeMillis() - start;
        if (scanSpec.length == 0 && end > TimeUnit.SECONDS.toMillis(10))
        {
            logger.info("Took " + end + "ms to scan for Actor implementations, for better performance "
                    + "set the property orbit.actors.basePackages");
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Took " + end + "ms to scan for Actor implementations.");
        }
        return Task.done();
    }

    private String[] extractScanSpec(final String[] actorBasePackages)
    {
        if (actorBasePackages.length > 0)
        {
            Set<String> tmp = new LinkedHashSet<>(actorBasePackages.length);
            for (String actorBasePackage : actorBasePackages)
            {
                if (!actorBasePackage.trim().isEmpty())
                {
                    tmp.add(actorBasePackage.trim());
                }
            }
            if (tmp.size() > 0) { // only create new scanSpec if valid actorBasePackage is passed in
                tmp.add("cloud.orbit"); // internal actors
                return tmp.toArray(new String[tmp.size()]);
            }
        }
        return actorBasePackages; // scan entire classpath
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Actor> Class<? extends T> findActorImplementation(Class<T> actorInterface)
    {
        return (Class<? extends T>) concreteImplementations.get(actorInterface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Actor> Collection<Class<? extends T>> findActorInterfaces(final Predicate<Class<T>> predicate)
    {
        List<Class<? extends T>> interfaces = null;
        for (Class<?> concreteInterface : concreteImplementations.keySet())
        {
            if (predicate.test((Class<T>) concreteInterface))
            {
                if (interfaces == null) {
                    interfaces = new ArrayList<>();
                }
                interfaces.add((Class<T>) concreteInterface);
            }
        }
        return interfaces != null ? interfaces : Collections.emptyList();
    }
}