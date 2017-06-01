/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.actors.extensions.MessageSerializer;
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
import de.javakaffee.kryoserializers.UUIDSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.UUID;

/**
 * Kryo based message serializer and object cloning implementation.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class KryoSerializer implements ExecutionObjectCloner, MessageSerializer
{
    private final KryoPool kryoPool;
    
    public KryoSerializer() {
        KryoFactory factory = new KryoFactory() {
            @Override
            public Kryo create() {
                Kryo kryo = new Kryo(new DefaultClassResolver() {
                    @Override
                    public Registration writeClass(Output output, Class type) {
                        if (type != null && !type.isInterface()) {
                            if (ActorObserver.class.isAssignableFrom(type)) {
                                super.writeClass(output, ActorObserver.class);
                                return kryo.getRegistration(type);
                            } else if (AbstractActor.class.isAssignableFrom(type)) {
                                super.writeClass(output, AbstractActor.class);
                                return kryo.getRegistration(type);
                            } else if (RemoteReference.class.isAssignableFrom(type)) {
                                super.writeClass(output, RemoteReference.class);
                                return kryo.getRegistration(type);
                            }
                        }
                        return super.writeClass(output, type);
                    }
                }, new MapReferenceResolver(), new DefaultStreamFactory());

                // Configure Kryo to first try to find and use a no-arg constructor and if it fails to do so,
                // fallback to StdInstantiatorStrategy (no constructor is invoked!).
                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
                
                kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
                kryo.register(Collections.emptyList().getClass(), new CollectionsEmptyListSerializer());
                kryo.register(Collections.emptyMap().getClass(), new CollectionsEmptyMapSerializer());
                kryo.register(Collections.emptySet().getClass(), new CollectionsEmptySetSerializer());
                kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
                kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
                kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
                kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
                kryo.register(InvocationHandler.class, new JdkProxySerializer());
                
                UnmodifiableCollectionsSerializer.registerSerializers(kryo);
                SynchronizedCollectionsSerializer.registerSerializers(kryo);

                kryo.register(UUID.class, new UUIDSerializer());
    
                // addDefaultSerializer for subclasses
                kryo.addDefaultSerializer(RemoteReference.class, new RemoteReferenceSerializer());
                kryo.addDefaultSerializer(AbstractActor.class, new AbstractActorSerializer());
                kryo.addDefaultSerializer(ActorObserver.class, new ActorObserverSerializer());
                
                kryo.register(ReferenceReplacement.class, new ReferenceReplacementSerializer());

                return kryo;
            }
        };
        
        kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }
         
    private static class ReferenceReplacementSerializer extends Serializer<ReferenceReplacement> {
        
        private ReferenceReplacementSerializer() {
            setImmutable(true);
        }
        
        @Override
        public void write(Kryo kryo, Output output, ReferenceReplacement object) {
            kryo.writeClass(output, object.interfaceClass);
            kryo.writeClassAndObject(output, object.id);
            if (object.address != null) {
                UUID uuid = object.address.asUUID();
                output.writeLong(uuid.getMostSignificantBits());
                output.writeLong(uuid.getLeastSignificantBits());
            } else {
                output.writeLong(0L);
                output.writeLong(0L);
            }
        }
    
        @Override
        public ReferenceReplacement read(Kryo kryo, Input input, Class<ReferenceReplacement> type) {
            ReferenceReplacement referenceReplacement = new ReferenceReplacement();
            referenceReplacement.interfaceClass = kryo.readClass(input).getType();
            referenceReplacement.id = kryo.readClassAndObject(input);
            referenceReplacement.address = readNodeAddress(input);
            return referenceReplacement;
        }
    }
    
    private static class AbstractActorSerializer extends Serializer {

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

        @Override
        public void write(Kryo kryo, Output output, Object object)
        {
            RemoteReference reference = ((AbstractActor) object).reference;
            ReferenceReplacement replacement = new ReferenceReplacement();
            replacement.address = reference.address;
            replacement.interfaceClass = reference._interfaceClass();
            replacement.id = reference.id;
            kryo.writeObject(output, replacement);
        }
    
        @Override
        public Object read(Kryo kryo, Input input, Class type)
        {
            ReferenceReplacement replacement = kryo.readObject(input, ReferenceReplacement.class);
            return BasicRuntime.getRuntime().getReference(replacement.interfaceClass, replacement.id);
        }
    }
    
    private static class ActorObserverSerializer extends Serializer
    {
        
        @Override
        public Object copy(Kryo kryo, Object original)
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
        
        @Override
        public void write(Kryo kryo, Output output, Object object)
        {
            ActorObserver objectReference = BasicRuntime.getRuntime().registerObserver(null, (ActorObserver) object);
            RemoteReference reference = (RemoteReference) objectReference;
            ReferenceReplacement replacement = new ReferenceReplacement();
            replacement.address = reference.address;
            replacement.interfaceClass = reference._interfaceClass();
            replacement.id = reference.id;
            kryo.writeObject(output, replacement);
        }
        
        @Override
        public Object read(Kryo kryo, Input input, Class type)
        {
            ReferenceReplacement replacement = kryo.readObject(input, ReferenceReplacement.class);
            if (replacement.address != null)
            {
                return BasicRuntime.getRuntime().getRemoteObserverReference(replacement.address, (Class<ActorObserver>) replacement.interfaceClass, replacement.id);
            }
            return BasicRuntime.getRuntime().getReference(replacement.interfaceClass, replacement.id);
        }
    }
    
    private static class RemoteReferenceSerializer extends Serializer {

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
        
        @Override
        public void write(Kryo kryo, Output output, Object object) {
            RemoteReference reference = (RemoteReference) object;
            ReferenceReplacement replacement = new ReferenceReplacement();
            replacement.address = reference.address;
            replacement.interfaceClass = reference._interfaceClass();
            replacement.id = reference.id;
            kryo.writeObject(output, replacement);
        }
        
        @Override
        public RemoteReference read(Kryo kryo, Input input, Class type) {
            ReferenceReplacement replacement = kryo.readObject(input, ReferenceReplacement.class);
            return BasicRuntime.getRuntime().getReference((Class<RemoteReference>) replacement.interfaceClass, replacement.id);
        }
    }
    
    private static NodeAddress readNodeAddress(Input in) {
        long most = in.readLong();
        long least = in.readLong();
        return most != 0L && least != 0L ? new NodeAddressImpl(new UUID(most, least)) : null;
    }
    
    private static class ReferenceReplacement implements Serializable {
        private static final long serialVersionUID = 1L;
        Class<?> interfaceClass;
        Object id;
        NodeAddress address;
    }
    
    @Override
    public Message deserializeMessage(BasicRuntime basicRuntime, InputStream inputStream) throws Exception {
        return kryoPool.run(kryo -> {
            try (Input in = new Input(inputStream)) {
                Message message = new Message();
                message.setMessageType(in.readByte());
                message.setMessageId(in.readInt());
                message.setReferenceAddress(readNodeAddress(in));
                message.setInterfaceId(in.readInt());
                message.setMethodId(in.readInt());
                message.setObjectId(kryo.readClassAndObject(in));
                message.setHeaders(kryo.readObjectOrNull(in, HashMap.class));
                message.setFromNode(readNodeAddress(in));
                message.setPayload(kryo.readClassAndObject(in));
                return message;
            } finally {
                kryo.reset();
            }
        });
    }
    
    @Override
    public void serializeMessage(BasicRuntime basicRuntime, OutputStream outputStream, Message message) throws Exception {
        kryoPool.run(kryo -> {
            try (Output out = new Output(outputStream)) {
                out.writeByte(message.getMessageType());
                out.writeInt(message.getMessageId());
                if (message.getReferenceAddress() != null) {
                    UUID uuid = message.getReferenceAddress().asUUID();
                    out.writeLong(uuid.getMostSignificantBits());
                    out.writeLong(uuid.getLeastSignificantBits());
                } else {
                    out.writeLong(0L);
                    out.writeLong(0L);
                }
                
                out.writeInt(message.getInterfaceId());
                out.writeInt(message.getMethodId());
                
                kryo.writeClassAndObject(out, message.getObjectId());
                kryo.writeObjectOrNull(out, message.getHeaders(), HashMap.class);
                
                if (message.getFromNode() != null) {
                    UUID nodeUuid = message.getFromNode().asUUID();
                    out.writeLong(nodeUuid.getMostSignificantBits());
                    out.writeLong(nodeUuid.getLeastSignificantBits());
                } else {
                    out.writeLong(0L);
                    out.writeLong(0L);
                }
    
                kryo.writeClassAndObject(out, message.getPayload());
                return null;
            } finally {
                kryo.reset();
            }
        });
    }
    
    @Override
    public <T> T clone(final T object) {
        if (object != null) {
            return kryoPool.run(kryo -> {
                try {
                    return kryo.copy(object);
                } finally {
                    kryo.reset();
                }
            });
        }
        return null;
    }
}
