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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.annotation.NoIdentity;
import com.ea.orbit.actors.cluster.NodeAddressImpl;
import com.ea.orbit.instrumentation.ClassPathUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReferenceFactory implements RefFactory
{
    private static ReferenceFactory instance = new ReferenceFactory();
    private ConcurrentMap<Class<?>, ActorFactory<?>> factories = new ConcurrentHashMap<>();
    private volatile ActorFactoryGenerator dynamicReferenceFactory;

    @Override
    public <T extends Actor> T getReference(final Class<T> iClass, final Object id)
    {
        ActorFactory<T> factory = getFactory(iClass);
        return factory.createReference(String.valueOf(id));
    }

    @Override
    public <T extends ActorObserver> T getObserverReference(final UUID nodeId, final Class<T> iClass, final Object id)
    {
        ActorFactory<T> factory = getFactory(iClass);
        final T reference = factory.createReference(String.valueOf(id));
        ActorReference.setAddress((ActorReference<?>) reference, new NodeAddressImpl(nodeId));
        return reference;
    }

    @SuppressWarnings("unchecked")
    private <T> ActorFactory<T> getFactory(final Class<T> iClass)
    {
        ActorFactory<T> factory = (ActorFactory<T>) factories.get(iClass);
        if (factory == null)
        {
            try
            {
                String factoryClazz = iClass.getSimpleName() + "Factory";
                if (factoryClazz.charAt(0) == 'I')
                {
                    factoryClazz = factoryClazz.substring(1); // remove leading 'I'
                }
                factory = (ActorFactory<T>) Class.forName(ClassPathUtils.getNullSafePackageName(iClass) + "." + factoryClazz).newInstance();
            }
            catch (Exception e)
            {
                if (dynamicReferenceFactory == null)
                {
                    dynamicReferenceFactory = new ActorFactoryGenerator();
                }
                factory = dynamicReferenceFactory.getFactoryFor(iClass);
            }

            factories.put(iClass, factory);
        }
        return factory;
    }

    public static <T extends Actor> T ref(Class<T> actorInterface, String id)
    {
        if (actorInterface.isAnnotationPresent(NoIdentity.class))
        {
            throw new IllegalArgumentException("Shouldn't supply ids for Actors annotated with " + NoIdentity.class);
        }
        return instance.getReference(actorInterface, id);
    }

    public static <T extends Actor> T ref(Class<T> actorInterface)
    {
        if (!actorInterface.isAnnotationPresent(NoIdentity.class))
        {
            throw new IllegalArgumentException("Not annotated with " + NoIdentity.class);
        }
        return instance.getReference(actorInterface, NoIdentity.NO_IDENTITY);
    }

    public static <T extends ActorObserver> T observerRef(UUID nodeId, Class<T> actorObserverInterface, String id)
    {
        return instance.getObserverReference(nodeId, actorObserverInterface, id);
    }

}
