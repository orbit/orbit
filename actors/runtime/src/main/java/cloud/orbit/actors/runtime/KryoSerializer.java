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
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Kryo based message serializer and object cloning implementation.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class KryoSerializer implements ExecutionObjectCloner, MessageSerializer
{
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final KryoPool kryoPool;
    private final KryoOutputPool outputPool = new KryoOutputPool();
    private final KryoInputPool inputPool = new KryoInputPool();

    public KryoSerializer()
    {
        this(kryo -> {});
    }

    public KryoSerializer(Consumer<Kryo> kryoConsumer)
    {
        KryoFactory factory = new KryoFactory()
        {
            @Override
            public Kryo create()
            {
                Kryo kryo = new Kryo(new DefaultClassResolver()
                {
                    @Override
                    public Registration writeClass(Output output, Class type)
                    {
                        if (type != null && !type.isInterface())
                        {
                            if (ActorObserver.class.isAssignableFrom(type))
                            {
                                super.writeClass(output, ActorObserver.class);
                                return kryo.getRegistration(type);
                            }
                            else if (AbstractActor.class.isAssignableFrom(type))
                            {
                                super.writeClass(output, AbstractActor.class);
                                return kryo.getRegistration(type);
                            }
                            else if (RemoteReference.class.isAssignableFrom(type))
                            {
                                super.writeClass(output, RemoteReference.class);
                                return kryo.getRegistration(type);
                            }
                        }
                        return super.writeClass(output, type);
                    }
                }, new MapReferenceResolver(), new DefaultStreamFactory());

                kryo.setAutoReset(true);

                // Configure Kryo to first try to find and use a no-arg constructor and if it fails to do so,
                // fallback to StdInstantiatorStrategy (no constructor is invoked!).
                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

                kryo.register(Object[].class);

                kryo.register(ReferenceReplacement.class, new ReferenceReplacementSerializer());

                // addDefaultSerializer for subclasses
                kryo.addDefaultSerializer(RemoteReference.class, new RemoteReferenceSerializer());
                kryo.addDefaultSerializer(AbstractActor.class, new AbstractActorSerializer());
                kryo.addDefaultSerializer(ActorObserver.class, new ActorObserverSerializer());

                kryo.register(UUID.class, new UUIDSerializer());

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

                kryoConsumer.accept(kryo);

                return kryo;
            }
        };

        kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }

    private static class ReferenceReplacementSerializer extends Serializer<ReferenceReplacement>
    {

        private ReferenceReplacementSerializer()
        {
            setImmutable(true);
        }

        @Override
        public void write(Kryo kryo, Output output, ReferenceReplacement object)
        {
            kryo.writeClass(output, object.interfaceClass);
            kryo.writeClassAndObject(output, object.id);
            writeNodeAddress(output, object.address);
        }

        @Override
        public ReferenceReplacement read(Kryo kryo, Input input, Class<ReferenceReplacement> type)
        {
            ReferenceReplacement referenceReplacement = new ReferenceReplacement();
            referenceReplacement.interfaceClass = kryo.readClass(input).getType();
            referenceReplacement.id = kryo.readClassAndObject(input);
            referenceReplacement.address = readNodeAddress(input);
            return referenceReplacement;
        }
    }

    private static class AbstractActorSerializer extends Serializer
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

    private static class RemoteReferenceSerializer extends Serializer
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

        @Override
        public void write(Kryo kryo, Output output, Object object)
        {
            RemoteReference reference = (RemoteReference) object;
            ReferenceReplacement replacement = new ReferenceReplacement();
            replacement.address = reference.address;
            replacement.interfaceClass = reference._interfaceClass();
            replacement.id = reference.id;
            kryo.writeObject(output, replacement);
        }

        @Override
        public RemoteReference read(Kryo kryo, Input input, Class type)
        {
            ReferenceReplacement replacement = kryo.readObject(input, ReferenceReplacement.class);
            return BasicRuntime.getRuntime().getReference((Class<RemoteReference>) replacement.interfaceClass, replacement.id);
        }
    }

    private static void writeNodeAddress(Output out, NodeAddress nodeAddress)
    {
        if (nodeAddress != null)
        {
            UUID uuid = nodeAddress.asUUID();
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
        }
        else
        {
            out.writeLong(0L);
            out.writeLong(0L);
        }
    }

    private static NodeAddress readNodeAddress(Input in)
    {
        long most = in.readLong();
        long least = in.readLong();
        return most != 0L && least != 0L ? new NodeAddressImpl(new UUID(most, least)) : null;
    }

    private static final class ReferenceReplacement implements Serializable
    {
        private static final long serialVersionUID = 1L;
        Class<?> interfaceClass;
        Object id;
        NodeAddress address;
    }

    private enum PayloadType
    {
        UNKNOWN(0),
        NULL(1),
        OBJECT_ARRAY(2);

        private final byte id;

        PayloadType(int id)
        {
            this.id = (byte) id;
        }
    }

    private enum ValueType
    {
        UNKNOWN(0),
        STRING(1),
        INT(2);

        private final byte id;

        ValueType(int id)
        {
            this.id = (byte) id;
        }

        public static ValueType getType(Object object)
        {
            if (String.class.isInstance(object))
            {
                return STRING;
            }
            if (Integer.class.isInstance(object) || int.class.isInstance(object))
            {
                return INT;
            }
            return UNKNOWN;
        }
    }

    @Override
    public Message deserializeMessage(BasicRuntime basicRuntime, InputStream inputStream) throws Exception
    {
        return inputPool.run(in -> {
            in.setInputStream(inputStream);
            return kryoPool.run(kryo ->
            {
                Message message = new Message();
                message.setMessageType(in.readByte());
                message.setMessageId(in.readInt());
                message.setReferenceAddress(readNodeAddress(in));
                message.setInterfaceId(in.readInt());
                message.setMethodId(in.readInt());
                message.setObjectId(readObjectId(kryo, in));
                message.setHeaders(readHeaders(kryo, in));
                message.setFromNode(readNodeAddress(in));
                message.setPayload(readPayload(kryo, in));
                return message;
            });
        }, DEFAULT_BUFFER_SIZE);
    }

    private static Object readPayload(Kryo kryo, Input in)
    {
        byte payloadTypeId = in.readByte();
        if (PayloadType.OBJECT_ARRAY.id == payloadTypeId)
        {
            return kryo.readObject(in, Object[].class);
        }
        if (PayloadType.NULL.id == payloadTypeId)
        {
            return null;
        }
        return kryo.readClassAndObject(in);
    }

    private static Map<String, Object> readHeaders(Kryo kryo, Input in)
    {
        int headers = in.readInt();
        if (headers == 0)
        {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new HashMap<>(headers);
        for (int i = 0; i < headers; i++)
        {
            String key = in.readString();
            byte valueTypeId = in.readByte();
            if (valueTypeId == ValueType.STRING.id)
            {
                payload.put(key, in.readString());
            }
            else if (valueTypeId == ValueType.INT.id)
            {
                payload.put(key, in.readInt());
            }
            else
            {
                payload.put(key, kryo.readClassAndObject(in));
            }
        }
        return payload;
    }

    private static Object readObjectId(Kryo kryo, Input in)
    {
        byte valueTypeIdForObjectId = in.readByte();
        if (valueTypeIdForObjectId == ValueType.STRING.id)
        {
            return in.readString();
        }
        else if (valueTypeIdForObjectId == ValueType.INT.id)
        {
            return in.readInt();
        }
        else
        {
            return kryo.readClassAndObject(in);
        }
    }

    @Override
    public byte[] serializeMessage(BasicRuntime basicRuntime, Message message) throws Exception
    {
        return outputPool.run(out ->
        {
            return kryoPool.run(kryo ->
            {
                out.writeByte(message.getMessageType());
                out.writeInt(message.getMessageId());
                writeNodeAddress(out, message.getReferenceAddress());
                out.writeInt(message.getInterfaceId());
                out.writeInt(message.getMethodId());
                writeObjectId(kryo, out, message);
                writeHeaders(kryo, out, message.getHeaders());
                writeNodeAddress(out, message.getFromNode());
                writePayload(kryo, out, message);
                return out.toBytes();
            });
        }, DEFAULT_BUFFER_SIZE);
    }

    private static void writePayload(Kryo kryo, Output out, Message message)
    {
        if (message.getPayload() instanceof Object[])
        {
            out.writeByte(PayloadType.OBJECT_ARRAY.id);
            kryo.writeObject(out, message.getPayload());
            return;
        }
        if (message.getPayload() == null)
        {
            out.writeByte(PayloadType.NULL.id);
            return;
        }
        out.writeByte(PayloadType.UNKNOWN.id);
        kryo.writeClassAndObject(out, message.getPayload());
    }

    private static void writeHeaders(Kryo kryo, Output out, Map<String, Object> headers)
    {
        if (headers == null || headers.isEmpty())
        {
            out.writeInt(0);
            return;
        }
        out.writeInt(headers.size());
        for (Map.Entry<String, Object> entry : headers.entrySet())
        {
            out.writeString(entry.getKey());
            ValueType valueType = ValueType.getType(entry.getValue());
            out.writeByte(valueType.id);
            if (valueType.equals(ValueType.STRING))
            {
                out.writeString(String.valueOf(entry.getValue()));
            }
            else if (valueType.equals(ValueType.INT))
            {
                out.writeInt((Integer) entry.getValue());
            }
            else
            {
                kryo.writeClassAndObject(out, entry.getValue());
            }
        }
    }

    private static void writeObjectId(Kryo kryo, Output out, Message message)
    {
        ValueType valueTypeForObjectId = ValueType.getType(message.getObjectId());
        out.writeByte(valueTypeForObjectId.id);
        if (valueTypeForObjectId.equals(ValueType.STRING))
        {
            out.writeString(String.valueOf(message.getObjectId()));
        }
        else if (valueTypeForObjectId.equals(ValueType.INT))
        {
            out.writeInt((Integer) message.getObjectId());
        }
        else
        {
            kryo.writeClassAndObject(out, message.getObjectId());
        }
    }

    @Override
    public <T> T clone(final T object)
    {
        if (object != null)
        {
            return kryoPool.run(kryo -> kryo.copy(object));
        }
        return null;
    }
}
