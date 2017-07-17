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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.annotation.NoIdentity;
import cloud.orbit.actors.annotation.OneWay;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.exception.UncheckedException;
import cloud.orbit.util.ClassPathUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultDescriptorFactory implements DescriptorFactory
{
    private static DefaultDescriptorFactory instance = new DefaultDescriptorFactory();

    private ConcurrentMap<Class<?>, ReferenceFactory<?>> factories = new ConcurrentHashMap<>();
    private ActorFactoryGenerator dynamicReferenceFactory = new ActorFactoryGenerator();

    private final ConcurrentMap<Class<?>, ClassDescriptor> descriptorMapByInterface = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ClassDescriptor> descriptorMapByInterfaceId = new ConcurrentHashMap<>();

    static class ClassDescriptor
    {
        ReferenceFactory<?> factory;
        ObjectInvoker<Object> invoker;
        boolean isObserver;
    }

    private DefaultDescriptorFactory()
    {

    }

    public static DescriptorFactory get()
    {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private ClassDescriptor getDescriptor(final Class<?> aInterface)
    {
        ClassDescriptor descriptor = descriptorMapByInterface.get(aInterface);
        if (descriptor == null)
        {
            if (aInterface == Actor.class || aInterface == ActorObserver.class)
            {
                return null;
            }

            descriptor = new ClassDescriptor();
            descriptor.isObserver = ActorObserver.class.isAssignableFrom(aInterface);
            if (aInterface.isInterface())
            {
                descriptor.factory = dynamicReferenceFactory.getFactoryFor(aInterface);
                descriptor.invoker = (ObjectInvoker<Object>) descriptor.factory.getInvoker();
            }

            ClassDescriptor concurrentInterfaceDescriptor = descriptorMapByInterface.putIfAbsent(aInterface, descriptor);
            if (concurrentInterfaceDescriptor != null)
            {
                descriptorMapByInterfaceId.put(DefaultClassDictionary.get().getClassId(aInterface), concurrentInterfaceDescriptor);
                return concurrentInterfaceDescriptor;
            }
            descriptorMapByInterfaceId.put(DefaultClassDictionary.get().getClassId(aInterface), descriptor);
        }
        return descriptor;
    }

    private ClassDescriptor getDescriptor(final int interfaceId)
    {
        final ClassDescriptor interfaceDescriptor = descriptorMapByInterfaceId.get(interfaceId);
        if (interfaceDescriptor != null)
        {
            return interfaceDescriptor;
        }
        Class clazz = findClassById(interfaceId);
        return clazz != null ? getDescriptor(clazz) : null;
    }

    private Class findClassById(final int interfaceId)
    {
        return DefaultClassDictionary.get().getClassById(interfaceId);
    }

    @Override
    public ObjectInvoker<?> getInvoker(Class clazz)
    {
        final ClassDescriptor descriptor = getDescriptor(clazz);
        if (descriptor == null)
        {
            return null;
        }
        if (descriptor.invoker == null)
        {
            descriptor.invoker = dynamicReferenceFactory.getInvokerFor(clazz);
        }
        return descriptor.invoker;
    }

    public ObjectInvoker<?> getInvoker(final int interfaceId)
    {
        final ClassDescriptor descriptor = getDescriptor(interfaceId);
        if (descriptor == null)
        {
            return getInvoker(DefaultClassDictionary.get().getClassById(interfaceId));
        }
        if (descriptor.invoker == null)
        {
            descriptor.invoker = dynamicReferenceFactory.getInvokerFor(descriptor.factory.getInterface());
        }
        return descriptor.invoker;
    }

    @Override
    public <T> T getReference(BasicRuntime runtime, final NodeAddress nodeId, final Class<T> iClass, final Object id)
    {
        ReferenceFactory<T> factory = getFactory(iClass);
        final T reference = factory.createReference(String.valueOf(id));
        if (nodeId != null)
        {
            RemoteReference.setAddress((RemoteReference<?>) reference, nodeId);
        }
        if (runtime != null)
        {
            ((RemoteReference) reference).runtime = runtime;
        }
        return reference;
    }

    @SuppressWarnings("unchecked")
    public <T> ReferenceFactory<T> getFactory(final Class<T> iClass)
    {
        ReferenceFactory<T> factory = (ReferenceFactory<T>) factories.get(iClass);
        if (factory == null)
        {
            if (!iClass.isInterface())
            {
                throw new IllegalArgumentException("Expecting an interface, but got: " + iClass.getName());
            }
            try
            {
                String factoryClazz = iClass.getSimpleName() + "Factory";
                if (factoryClazz.charAt(0) == 'I')
                {
                    factoryClazz = factoryClazz.substring(1); // remove leading 'I'
                }
                factory = (ReferenceFactory<T>) Class.forName(ClassPathUtils.getNullSafePackageName(iClass) + "." + factoryClazz).newInstance();
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

    @SuppressWarnings("unchecked")
    public static <T extends Actor> T ref(int interfaceId, String id)
    {
        final Class classById = instance.findClassById(interfaceId);
        if (classById == null)
        {
            throw new UncheckedException("Class not found, id: " + interfaceId);
        }
        return (T) ref(classById, id);
    }

    public static <T extends Actor> T ref(Class<T> actorInterface, String id)
    {
        if (id != null)
        {
            if (actorInterface.isAnnotationPresent(NoIdentity.class))
            {
                throw new IllegalArgumentException("Shouldn't supply ids for Actors annotated with " + NoIdentity.class);
            }
        }
        else if (!actorInterface.isAnnotationPresent(NoIdentity.class))
        {
            throw new IllegalArgumentException("Not annotated with " + NoIdentity.class);
        }
        return instance.getReference(null, actorInterface, id);
    }

    public static <T extends ActorObserver> T observerRef(NodeAddress nodeId, Class<T> actorObserverInterface, String id)
    {
        return instance.getReference(nodeId, actorObserverInterface, id);
    }


    @SuppressWarnings("unchecked")
    public static <T> T cast(Class<T> remoteInterface, Actor actor)
    {
        return (T) Proxy.newProxyInstance(DefaultDescriptorFactory.class.getClassLoader(), new Class[]{ remoteInterface },
                (proxy, method, args) -> {
                    // TODO: throw proper exceptions for the expected error scenarios (non task return),
                    final int methodId = instance.dynamicReferenceFactory.getMethodId(method);
                    return ActorRuntime.getRuntime()
                            .invoke(RemoteReference.from(actor), method, method.isAnnotationPresent(OneWay.class), methodId, args);
                });

    }
}
