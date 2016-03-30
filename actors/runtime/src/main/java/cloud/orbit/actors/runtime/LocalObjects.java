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
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.transactions.IdUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskFunction;
import cloud.orbit.exception.NotImplementedException;
import cloud.orbit.exception.UncheckedException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class LocalObjects
{
    private ConcurrentMap<Object, LocalObjectEntry> localObjects = new ConcurrentHashMap<>();
    private Cache<Object, LocalObjectEntry> objectMap = CacheBuilder.newBuilder().weakKeys()
            .removalListener(this::onRemoval).build();


    // used for searches in localObjects
    private static class ObjectKey extends RemoteReference<Object>
    {
        private final int interfaceId;

        public ObjectKey(NodeAddress address, int interfaceId, Object id)
        {
            super(id);
            this.address = address;
            this.interfaceId = interfaceId;
        }

        public ObjectKey(int interfaceId, Object id)
        {
            super(id);
            this.interfaceId = interfaceId;
        }


        @Override
        protected int _interfaceId()
        {
            return interfaceId;
        }

        @Override
        protected Class<Object> _interfaceClass()
        {
            // this never supposed to be called;
            throw new NotImplementedException("_interfaceClass() is not implemented");
        }
    }

    public interface LocalObjectEntry<T>
    {
        RemoteReference<T> getRemoteReference();

        default boolean isDeactivated()
        {
            return false;
        }

        T getObject();

        <R> Task<R> run(TaskFunction<LocalObjectEntry<T>, R> function);
    }

    public static class NormalObjectEntry<T> implements LocalObjectEntry<T>
    {
        protected RemoteReference<T> reference;
        protected T object;

        @Override
        public T getObject()
        {
            return object;
        }

        @Override
        public <R> Task<R> run(final TaskFunction<LocalObjectEntry<T>, R> function)
        {
            return function.apply(this);
        }

        @Override
        public RemoteReference<T> getRemoteReference()
        {
            return reference;
        }
    }

    public LocalObjectEntry findLocalObject(NodeAddress address, int interfaceId, Object objectId)
    {
        return localObjects.get(new ObjectKey(address, interfaceId, objectId));
    }

    public LocalObjectEntry findLocalObjectByReference(RemoteReference reference)
    {
        return localObjects.get(reference);
    }

    public LocalObjectEntry findLocalActor(Actor actor)
    {
        if (actor instanceof AbstractActor)
        {
            return (LocalObjectEntry) ((AbstractActor) actor).activation;
        }
        return localObjects.get(RemoteReference.from(actor));
    }


    public LocalObjectEntry findLocalObjectByObject(Object object)
    {
        return objectMap.getIfPresent(object);
    }


    /**
     * Installs this remote object into this node with the given id.
     *
     * @param address  The local node address if this is a object that will not be registered in the directory
     * @param iClass   Hint to the framework about which remote interface this object represents.
     *                 Can be null if there are no ambiguities.
     * @param objectId Can be null, in this case the framework will choose an id.
     * @param object   The object to install
     * @param <T>      The type of reference class returned.
     * @return a remote reference that can be sent to actors.
     * @throws IllegalArgumentException if called twice with the same observer and different ids
     */
    @SuppressWarnings("unchecked")
    public <T> RemoteReference<T> getOrAddLocalObjectReference(NodeAddress address, Class<T> iClass, String objectId, final T object)
    {
        final LocalObjectEntry localObject = objectMap.getIfPresent(object);

        if (localObject != null)
        {
            RemoteReference ref = localObject.getRemoteReference();
            if (objectId != null && !java.util.Objects.equals(ref.id, objectId))
            {
                throw new IllegalArgumentException("Called twice with different ids: " + objectId + " != " + ((RemoteReference<?>) ref).id);
            }
            if (address != null && !java.util.Objects.equals(ref.address, address))
            {
                throw new IllegalArgumentException("Called twice with different addresses: " + address + " != " + ((RemoteReference<?>) ref).address);
            }
            if (iClass != null && !java.util.Objects.equals(ref._interfaceClass(), iClass))
            {
                throw new IllegalArgumentException("Called twice with different Classes: " + iClass + " != " + ((RemoteReference<?>) ref)._interfaceClass());
            }
            return (RemoteReference<T>) ref;
        }
        return registerLocalObject(address, iClass, objectId, object);
    }

    @SuppressWarnings("unchecked")
    protected <T> RemoteReference<T> registerLocalObject(NodeAddress address, Class<T> iClass, String objectId, final T object)
    {
        final LocalObjectEntry previousLocalObject = objectMap.getIfPresent(object);
        if (previousLocalObject != null)
        {
            // perhaps should just return instead? modify if later code introduce concurrency issues
            throw new IllegalArgumentException("Object already installed: " + previousLocalObject.getRemoteReference());
        }
        if (iClass == null)
        {
            iClass = (Class) findRemoteInterface(ActorObserver.class, object);
            if (iClass == null)
            {
                iClass = (Class) findRemoteInterface(Actor.class, object);
            }
        }
        if (iClass == null)
        {
            throw new IllegalArgumentException("Can't find a remote interface for " + object);
        }
        final String actualObjectId = objectId != null ? objectId : IdUtils.urlSafeString(128);


        RemoteReference reference = createReference(address, iClass, actualObjectId);
        return registerLocalObject(reference, object);
    }

    void registerEntry(Object key, LocalObjectEntry entry)
    {
        // used for stateless activations, for instance.
        objectMap.put(key, entry);
    }


    @SuppressWarnings("unchecked")
    public <T> RemoteReference<T> registerLocalObject(RemoteReference reference, final T object)
    {
        if (object != null)
        {
            final LocalObjectEntry previousLocalObject = objectMap.getIfPresent(object);
            if (previousLocalObject != null)
            {
                // perhaps should just return instead? modify if later code introduce concurrency issues
                throw new IllegalArgumentException("Object already installed: " + previousLocalObject.getRemoteReference());
            }
        }

        final LocalObjectEntry existing = localObjects.get(reference);
        if (existing != null)
        {
            throw new IllegalArgumentException("Object clashes with a pre-existing object: " + reference);
        }
        LocalObjectEntry localObject = createLocalObjectEntry(reference, object);
        try
        {
            if (object != null)
            {
                final LocalObjectEntry other = objectMap.get(object, () -> localObject);
                if (localObject != other)
                {
                    if (!Objects.equals(reference, other.getRemoteReference()))
                    {
                        throw new ConcurrentModificationException();
                    }
                    return (RemoteReference) other.getRemoteReference();
                }
            }
        }
        catch (ExecutionException e)
        {
            throw new UncheckedException(e);
        }
        final LocalObjectEntry previous = localObjects.putIfAbsent(reference, localObject);
        if (previous != null && localObject != previous)
        {
            if (!Objects.equals(reference, previous.getRemoteReference()))
            {
                throw new ConcurrentModificationException();
            }
            return (RemoteReference) previous.getRemoteReference();
        }
        return reference;
    }

    protected Class<?> findRemoteInterface(final Class<?> baseInterface, final Object instance)
    {
        for (Class<?> aInterface : instance.getClass().getInterfaces())
        {
            if (baseInterface.isAssignableFrom(aInterface))
            {
                return aInterface;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> RemoteReference<T> createReference(NodeAddress address, Class<T> iClass, String objectId)
    {
        final T reference = DefaultDescriptorFactory.get().getReference(iClass, objectId);
        if (address != null)
        {
            ((RemoteReference) reference).address = address;
        }
        return (RemoteReference<T>) reference;
    }

    /**
     * This method must be overridden by the stage to create proper entries for Actors and Stateless Actors
     *
     * @return a object handle
     */
    protected <T> LocalObjectEntry createLocalObjectEntry(final RemoteReference<T> reference, final T object)
    {
        final NormalObjectEntry localObject = new NormalObjectEntry();
        localObject.object = object;
        localObject.reference = reference;
        return localObject;
    }

    private void onRemoval(final RemovalNotification<Object, LocalObjectEntry> objectObjectRemovalNotification)
    {
        if (objectObjectRemovalNotification.getCause() == RemovalCause.COLLECTED)
        {
            final LocalObjectEntry objectEntry = objectObjectRemovalNotification.getValue();
            if (objectEntry != null && objectEntry.getRemoteReference() != null)
            {
                localObjects.remove(objectEntry.getRemoteReference());
            }
        }
    }

    public Stream<Map.Entry<Object, LocalObjectEntry>> stream()
    {
        return localObjects.entrySet().stream();
    }

    public int getLocalObjectCount()
    {
        return localObjects.size();
    }

    public void remove(Object key, Object object)
    {
        localObjects.remove(key, object);
    }

}
