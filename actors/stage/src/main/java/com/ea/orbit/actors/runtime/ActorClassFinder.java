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

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.providers.IActorClassFinder;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ClassPath;
import com.ea.orbit.util.IOUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActorClassFinder implements IActorClassFinder
{
    private static final ClassPathSearch search = new ClassPathSearch(IActor.class);

    private ConcurrentHashMap<Class<?>, Class<?>> cache = new ConcurrentHashMap<>();
    private Execution execution;

    public ActorClassFinder(Execution execution)
    {
        this.execution = execution;
    }

    public ActorClassFinder()
    {

    }

    @SuppressWarnings("unchecked")
	@Override
    public <T extends IActor> Class<? extends T> findActorImplementation(Class<T> iActorInterface)
    {
        Class<?> r = cache.get(iActorInterface);
        return r != null ? (Class<? extends T>) r : search.findImplementation(iActorInterface);
    }

    @Override
    public Task<?> start()
    {
        if (execution == null)
        {
            return Task.done();
        }
        try
        {
            if (execution.getAutoDiscovery())
            {
                final List<ClassPath.ResourceInfo> actorInterfacesRes = ClassPath.get().getAllResources().stream()
                        .filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/interfaces")).collect(Collectors.toList());
                final List<ClassPath.ResourceInfo> actorClassesRes = ClassPath.get().getAllResources().stream()
                        .filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/classes")).collect(Collectors.toList());

                for (ClassPath.ResourceInfo irs : actorInterfacesRes)
                {
                    // pre register factories
                    String nameFactoryName = IOUtils.toString(irs.url().openStream());
                    ActorFactory<?> factory = (ActorFactory<?>) classForName(nameFactoryName).newInstance();
                    execution.registerFactory(factory);
                }

                for (ClassPath.ResourceInfo irs : actorClassesRes)
                {
                    String className = irs.getResourceName().substring("META-INF/orbit/actors/classes".length() + 1);
                    Class<?> actorClass = classForName(className);
                    cacheBestInterface(actorClass);
                }
            }
            List<Class<?>> actorClasses = execution.getActorClasses();
            if (actorClasses != null && !actorClasses.isEmpty())
            {
                for (Class<?> actorClass : actorClasses)
                {
                    cacheBestInterface(actorClass);
                }
            }
        }

        catch (Throwable e)
        {
            throw new UncheckedException(e);
        }
        return Task.done();
    }

    private void cacheBestInterface(Class<?> actorClass)
    {
        Class<?> bestInterface = null;
        for (Class<?> interfaceClass : actorClass.getInterfaces())
        {
            if (IActor.class.isAssignableFrom(interfaceClass) && interfaceClass != IActor.class)
            {
                bestInterface = (bestInterface == null) ? interfaceClass
                        : bestInterface.isAssignableFrom(interfaceClass) ? interfaceClass
                        : bestInterface;
            }
        }
        if (bestInterface != null)
        {
            cache.put(actorClass, bestInterface);
        }
    }

    private Class<?> classForName(String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw new UncheckedException(e);
        }
    }

}