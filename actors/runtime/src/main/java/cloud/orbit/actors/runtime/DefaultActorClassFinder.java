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
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultActorClassFinder implements ActorClassFinder
{
    private static Logger logger = LoggerFactory.getLogger(DefaultActorClassFinder.class);

    private static final Map<Class<?>, Class<?>> concreteImplementations = new ConcurrentHashMap<>();

    static {
        String basePackage = System.getProperty("orbit.actors.basePackage", "");
        String[] scanSpec = new String[]{};
        if (!basePackage.isEmpty())
        {
            String[] basePackages = basePackage.split(",");
            Set<String> tmp = new LinkedHashSet<>(basePackages.length);
            for (String s : basePackages)
            {
                if (!s.trim().isEmpty())
                {
                    tmp.add(s);
                }
            }
            tmp.add("cloud.orbit"); // internal actors
            scanSpec = tmp.toArray(new String[tmp.size()]);
        }

        List<Class<?>> clazzInterfaces = new ArrayList<>();
        new FastClasspathScanner(scanSpec).matchSubinterfacesOf(Actor.class, candidate -> {
            if (candidate == Remindable.class)
            {
                return;
            }
            clazzInterfaces.add(candidate);
        }).scan();

        new FastClasspathScanner(scanSpec).matchClassesImplementing(Actor.class, clazzImplementation -> {
            for (Class<?> clazzInterface : clazzInterfaces)
            {
                if (clazzInterface.isAssignableFrom(clazzImplementation))
                {
                    Class<?> old = concreteImplementations.put(clazzInterface, clazzImplementation);
                    if (old != null)
                    {
                        logger.warn("Multiple actor implementations found for " + clazzInterface);
                    }
                }
            }
        }).scan();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Actor> Class<? extends T> findActorImplementation(Class<T> actorInterface)
    {
        return (Class<? extends T>) concreteImplementations.get(actorInterface);
    }
}