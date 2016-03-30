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

package cloud.orbit.actors.extensions.json;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.DescriptorFactory;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.exception.UncheckedException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;

import java.io.IOException;
import java.util.UUID;

/**
 * Used to replace actor and observer references with a single string in json.
 */
public class ActorReferenceModule extends Module
{
    /**
     * Version. Required by the Jackson module contract.
     */
    private static final Version MODULE_VERSION = new Version(1, 0, 0, "", "cloud.orbit.actors", "json");
    /**
     * Class that will create concrete references to actor interfaces
     */
    private DescriptorFactory descriptorFactory;

    public ActorReferenceModule()
    {
    }

    public ActorReferenceModule(final DescriptorFactory descriptorFactory)
    {
        super();
        this.descriptorFactory = descriptorFactory;
    }

    private static class RefSerializer extends JsonSerializer<Object> implements ContextualSerializer
    {
        private final Class<?> rawClass;

        public RefSerializer(final Class<?> rawClass)
        {
            if (rawClass != null && rawClass.isInterface() && Actor.class.isAssignableFrom(rawClass))
            {
                this.rawClass = rawClass;
            }
            else
            {
                this.rawClass = null;
            }
        }


        @Override
        public void serialize(final Object value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException
        {
            final RemoteReference<?> reference = (RemoteReference<?>)
                    (value instanceof AbstractActor ? RemoteReference.from((AbstractActor) value) : value);
            final String text = String.valueOf(RemoteReference.getId(reference));
            final Class<?> interfaceClass = RemoteReference.getInterfaceClass(reference);
            if (interfaceClass != null && (interfaceClass == rawClass))
            {
                // escape starting '!'
                if (!text.startsWith("!!"))
                {
                    jgen.writeString(text);
                }
                else
                {
                    jgen.writeString("!" + text);
                }

            }
            else if (interfaceClass != null && rawClass == null)
            {
                throw new IOException("Illegal json serialization of actor reference " + interfaceClass + ":" + text + " won't be able to deserialize later");
            }
            else
            {
                jgen.writeString("!!" + interfaceClass.getName() + " " + text);
            }
        }

        @Override
        public void serializeWithType(final Object value, final JsonGenerator gen, final SerializerProvider serializers, final TypeSerializer typeSer) throws IOException
        {
            serialize(value, gen, serializers);
        }

        @Override
        public JsonSerializer<?> createContextual(final SerializerProvider prov, final BeanProperty property) throws JsonMappingException
        {
            //return property.get;
            if (property != null)
            {
                final JavaType type = property.getType();
                if (type != null)
                {
                    final Class<?> clazz = type.getRawClass();
                    if (type.isCollectionLikeType())
                    {
                        return new RefSerializer(type.getContentType().getRawClass());
                    }
                    if (type.isMapLikeType())
                    {
                        return new RefSerializer(type.getContentType().getRawClass());
                    }
                    if (clazz != null)
                    {
                        return new RefSerializer(clazz);
                    }
                }
            }
            return this;
        }
    }

    private static class ObserverRefSerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(final Object value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException
        {
            final NodeAddress address = RemoteReference.getAddress((RemoteReference<?>) value);
            final Object actorId = RemoteReference.getId((RemoteReference<?>) value);
            jgen.writeString((address != null ? address.asUUID() : "") + "/" + actorId);
        }

        @Override
        public void serializeWithType(final Object value, final JsonGenerator gen, final SerializerProvider serializers, final TypeSerializer typeSer) throws IOException
        {
            serialize(value, gen, serializers);
        }
    }

    private static class RefDeserializer extends JsonDeserializer<Object>
    {
        private final Class<?> iClass;
        private final DescriptorFactory factory;

        public RefDeserializer(final Class<?> iClass, final DescriptorFactory factory)
        {
            super();
            this.iClass = iClass;
            this.factory = factory;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Object deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException
        {
            final String text = jsonParser.getText();
            if (text != null && text.startsWith("!!"))
            {
                if (text.startsWith("!!!"))
                {
                    // three "!!!" is the escape when the id starts with "!!"
                    return factory.getReference(null, (Class) iClass, text.substring(1));
                }
                int idx = text.indexOf(' ');
                String className = text.substring(2, idx);
                String key = text.substring(idx + 1);
                try
                {
                    final Class aClass = Class.forName(className);
                    return factory.getReference(null, aClass, key);
                }
                catch (ClassNotFoundException e)
                {
                    throw new UncheckedException("Can't find class: " + e);
                }
            }
            return factory.getReference(null, (Class) iClass, text);
        }

        @Override
        public Object deserializeWithType(final JsonParser p, final DeserializationContext ctxt, final TypeDeserializer typeDeserializer) throws IOException
        {
            return deserialize(p, ctxt);
        }
    }

    private static class ObserverRefDeserializer extends JsonDeserializer<Object>
    {
        private final Class<?> iClass;
        private final DescriptorFactory factory;

        public ObserverRefDeserializer(final Class<?> iClass, final DescriptorFactory factory)
        {
            super();
            this.iClass = iClass;
            this.factory = factory;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Object deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException
        {
            final String text = jsonParser.getText();
            final int idx = text.indexOf('/');
            if (idx != 0)
            {
                final UUID uuid = UUID.fromString(text.substring(0, idx));
                return factory.getReference(new NodeAddressImpl(uuid), (Class) iClass, text.substring(idx + 1));
            }
            else
            {
                return factory.getReference(null, (Class) iClass, text.substring(idx + 1));

            }
        }

        @Override
        public Object deserializeWithType(final JsonParser p, final DeserializationContext ctxt, final TypeDeserializer typeDeserializer) throws IOException
        {
            return deserialize(p, ctxt);
        }
    }

    @Override
    public String getModuleName()
    {
        return "orbit-actor-reference";
    }

    @Override
    public Version version()
    {
        return MODULE_VERSION;
    }

    @Override
    public void setupModule(final SetupContext context)
    {
        context.addDeserializers(new Deserializers.Base()
        {
            @Override
            public JsonDeserializer<?> findBeanDeserializer(final JavaType type, final DeserializationConfig config, final BeanDescription beanDesc)
            {
                final Class<?> rawClass = type.getRawClass();
                if (Actor.class.isAssignableFrom(rawClass))
                {
                    return new RefDeserializer(rawClass, descriptorFactory);
                }
                if (ActorObserver.class.isAssignableFrom(rawClass))
                {
                    return new ObserverRefDeserializer(rawClass, descriptorFactory);
                }
                return null;
            }
        });
        context.addSerializers(new Serializers.Base()
        {
            @Override
            public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc)
            {
                final Class<?> rawClass = type.getRawClass();
                if (Actor.class.isAssignableFrom(rawClass))
                {
                    return new RefSerializer(rawClass);
                }
                if (ActorObserver.class.isAssignableFrom(rawClass))
                {
                    return new ObserverRefSerializer();
                }
                return null;
            }
        });
    }

    public DescriptorFactory getDescriptorFactory()
    {
        return descriptorFactory;
    }

    public void setDescriptorFactory(final DescriptorFactory descriptorFactory)
    {
        this.descriptorFactory = descriptorFactory;
    }
}
