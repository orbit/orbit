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

package cloud.orbit.actors.cloner;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.RemoteReference;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Kyro based object cloning implementation.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class KryoCloner implements ExecutionObjectCloner
{
    private final KryoPool kryoPool;

    public KryoCloner()
    {
        KryoFactory factory = new KryoFactory()
        {
            @Override
            public Kryo create()
            {
                Kryo kryo = new Kryo();

                // Configure Kryo to first try to find and use a no-arg constructor and if it fails to do so,
                // fallback to StdInstantiatorStrategy (no constructor is invoked!).

                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

                kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
                kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
                kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
                kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
                kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
                kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
                kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
                kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
                kryo.register(InvocationHandler.class, new JdkProxySerializer());
                UnmodifiableCollectionsSerializer.registerSerializers(kryo);
                SynchronizedCollectionsSerializer.registerSerializers(kryo);

                kryo.addDefaultSerializer(RemoteReference.class, new DefaultSerializers.VoidSerializer()
                {
                    @Override
                    public Object copy(final Kryo kryo, final Object original)
                    {
                        if (original instanceof RemoteReference)
                        {
                            final RemoteReference<?> remoteReference = (RemoteReference<?>) original;
                            if (RemoteReference.getRuntime(remoteReference) != null)
                            {
                                return DefaultDescriptorFactory.get().getReference(null, RemoteReference.getAddress(remoteReference),
                                        RemoteReference.getInterfaceClass(remoteReference),
                                        RemoteReference.getId(remoteReference));
                            }
                        }
                        return original;
                    }
                });
                kryo.addDefaultSerializer(AbstractActor.class, new DefaultSerializers.VoidSerializer()
                {
                    @Override
                    public Object copy(final Kryo kryo, final Object original)
                    {
                        if (original instanceof AbstractActor)
                        {
                            return RemoteReference.from((AbstractActor) original);
                        }
                        if (original instanceof RemoteReference)
                        {

                            final RemoteReference<?> remoteReference = (RemoteReference<?>) original;
                            if (RemoteReference.getRuntime(remoteReference) != null)
                            {
                                return DefaultDescriptorFactory.get().getReference(null, RemoteReference.getAddress(remoteReference),
                                        RemoteReference.getInterfaceClass(remoteReference),
                                        RemoteReference.getId(remoteReference));
                            }
                            return original;
                        }
                        if (original == null)
                        {
                            return null;
                        }
                        throw new IllegalArgumentException("Invalid type for " + original);
                    }
                });
                kryo.addDefaultSerializer(ActorObserver.class, new DefaultSerializers.VoidSerializer()
                {
                    @Override
                    public Object copy(final Kryo kryo, final Object original)
                    {
                        if (original instanceof RemoteReference)
                        {
                            final RemoteReference<?> remoteReference = (RemoteReference<?>) original;
                            if (RemoteReference.getRuntime(remoteReference) != null)
                            {
                                return DefaultDescriptorFactory.get().getReference(null, RemoteReference.getAddress(remoteReference),
                                        RemoteReference.getInterfaceClass(remoteReference),
                                        RemoteReference.getId(remoteReference));
                            }
                            return original;
                        }
                        return ActorRuntime.getRuntime().registerObserver(null, (ActorObserver) original);
                    }
                });
                kryo.addDefaultSerializer(UUID.class, new DefaultSerializers.VoidSerializer());

                return kryo;
            }
        };

        kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }

    @Override
    public <T> T clone(final T object)
    {
        if (object != null)
        {
            return kryoPool.run(kryo -> {
                try
                {
                    return kryo.copy(object);
                }
                finally
                {
                    kryo.reset();
                }
            });
        }
        return null;
    }
}
