package com.ea.orbit.actors.extensions.json;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.actors.runtime.ObjectInvoker;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonMessageSerializer implements MessageSerializer
{
    private final ObjectMapper mapper = new ObjectMapper();
    private final ActorReferenceModule actorReferenceModule;
    private BasicRuntime runtime;

    public JsonMessageSerializer()
    {
        actorReferenceModule = new ActorReferenceModule(DefaultDescriptorFactory.get());
        mapper.registerModule(actorReferenceModule);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        TypeResolverBuilder<?> typer = new ClassIdTypeResolverBuilder(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);
    }


    @Override
    public Message deserializeMessage(final BasicRuntime runtime, final InputStream inputStream) throws Exception
    {
        ensureInit(runtime);
        try
        {
            final Message message = mapper.readValue(inputStream, Message.class);

            // decode payload parameters according to the interface/method
            if (message.getPayload() != null &&
                    (message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE
                            || message.getMessageType() == MessageDefinitions.REQUEST_MESSAGE))
            {
                final ObjectInvoker invoker = runtime.getInvoker(message.getInterfaceId());
                final Method method = invoker.getMethod(message.getMethodId());

                final Object[] args = castArgs(method.getGenericParameterTypes(), message.getPayload());
                message.setPayload(args);
            }
            return message;
        }
        catch (Exception ex)
        {
            throw ex;
        }

    }

    private void ensureInit(final BasicRuntime newRuntime)
    {
        if (runtime == null && newRuntime != null)
        {
            synchronized (actorReferenceModule)
            {
                if (runtime == null)
                {
                    actorReferenceModule.setDescriptorFactory(newRuntime);
                    runtime = newRuntime;
                }
            }
        }
    }

    @Override
    public void serializeMessage(final BasicRuntime runtime, final OutputStream out, final Message message) throws Exception
    {
        ensureInit(runtime);
        if (message.getPayload() instanceof Throwable && message.getMessageType() == MessageDefinitions.RESPONSE_ERROR)
        {
            final StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw))
            {
                ((Throwable) message.getPayload()).printStackTrace(pw);
                pw.flush();
            }
            message.withPayload(sw.toString());
        }
        mapper.writeValue(out, message);
    }

    private Object[] castArgs(final Type[] genericParameterTypes, final Object payload)
    {
        Object[] args0 = payload instanceof List ? ((List) payload).toArray() : (Object[]) payload;
        Object[] casted = new Object[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++)
        {
            casted[i] = convertValue(args0[i], genericParameterTypes[i]);
        }
        return casted;
    }

    private Object convertValue(final Object o, final Type genericParameterType)
    {
        if (genericParameterType == String.class)
        {
            return o == null ? null : String.valueOf(o);
        }
        if (genericParameterType == int.class && o instanceof Number)
        {
            return ((Number) o).intValue();
        }
        if (genericParameterType == Object.class)
        {
            return o;
        }
        try
        {
            return mapper.convertValue(o, mapper.constructType(genericParameterType));
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
            throw new UncheckedException(ex);
        }
    }
}

class ClassIdTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder
{

    public ClassIdTypeResolverBuilder(ObjectMapper.DefaultTyping typing)
    {
        super(typing);
    }

    public boolean useForType(JavaType t)
    {
        if (t.isCollectionLikeType())
        {
            return false;
        }
        return super.useForType(t);
    }


    @Override
    protected TypeIdResolver idResolver(final MapperConfig<?> config, final JavaType baseType, final Collection<NamedType> subtypes, final boolean forSer, final boolean forDeser)
    {
        if (_customIdResolver != null)
        {
            return _customIdResolver;
        }
        if (_idType == null) throw new IllegalStateException("Can not build, 'init()' not yet called");
        switch (_idType)
        {
            case CUSTOM:
            case NAME: // need custom resolver...
                return new ClassIdResolver(baseType, config);
        }
        return super.idResolver(config, baseType, subtypes, forSer, forDeser);
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(final DeserializationConfig config, final JavaType baseType, final Collection<NamedType> subtypes)
    {
        if (!useForType(baseType))
        {
            return null;
        }
        if (_idType == JsonTypeInfo.Id.NONE)
        {
            return null;
        }

        TypeIdResolver idRes = idResolver(config, baseType, subtypes, false, true);

        // First, method for converting type info to type id:
        switch (_includeAs)
        {
            case PROPERTY:
            case EXISTING_PROPERTY: // as per [#528] same class as PROPERTY
                return new ClassIdAsPropertyTypeDeserializer(baseType, _typeProperty, _typeIdVisible, _defaultImpl, _includeAs, idRes);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: " + _includeAs);

    }

    @Override
    public TypeSerializer buildTypeSerializer(final SerializationConfig config, final JavaType baseType, final Collection<NamedType> subtypes)
    {
        if (!useForType(baseType))
        {
            return null;
        }
        if (_idType == JsonTypeInfo.Id.NONE)
        {
            return null;
        }
        TypeIdResolver idRes = idResolver(config, baseType, subtypes, true, false);
        switch (_includeAs)
        {
            case PROPERTY:
            case EXISTING_PROPERTY:
                return new ClassIdAsPropertyTypeSerializer(idRes, _typeProperty);
        }
        return super.buildTypeSerializer(config, baseType, subtypes);
    }

}

class ClassIdResolver extends ClassNameIdResolver
{
    public ClassIdResolver(final JavaType baseType, final MapperConfig<?> config)
    {
        super(baseType, config.getTypeFactory());
    }

    @Override
    public String idFromValue(Object value)
    {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> type)
    {
        if (type.isArray())
        {
            return null;
        }
        if (value != null)
        {
            return String.valueOf(DefaultClassDictionary.get().getClassId(value.getClass()));
        }
        return String.valueOf(DefaultClassDictionary.get().getClassId(type));
    }

    @Override
    protected JavaType _typeFromId(final String id, final TypeFactory typeFactory)
    {
        Class<?> cls = DefaultClassDictionary.get().getClassById(Integer.parseInt(id));
        return typeFactory.constructSpecializedType(_baseType, cls);
    }
}

class ClassIdAsPropertyTypeDeserializer extends AsPropertyTypeDeserializer
{
    public ClassIdAsPropertyTypeDeserializer(AsPropertyTypeDeserializer src, BeanProperty property)
    {
        super(src, property);
    }

    public ClassIdAsPropertyTypeDeserializer(final JavaType baseType, final String _typeProperty, final boolean _typeIdVisible, final Class<?> _defaultImpl, final JsonTypeInfo.As _includeAs, final TypeIdResolver idRes)
    {
        super(baseType, idRes, _typeProperty, _typeIdVisible, _defaultImpl, _includeAs);
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop)
    {
        return (prop == _property) ? this : new ClassIdAsPropertyTypeDeserializer(this, prop);
    }

    protected String _locateTypeId(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        if (!jp.isExpectedStartArrayToken())
        {
            if (_defaultImpl != null)
            {
                return _idResolver.idFromBaseType();
            }
            throw ctxt.wrongTokenException(jp, JsonToken.START_ARRAY, "Missing type information for: " + baseTypeName());
        }

        return String.valueOf(DefaultClassDictionary.get().getClassId(ArrayList.class));
    }

    protected Object _deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        if (jp.canReadTypeId())
        {
            Object typeId = jp.getTypeId();
            if (typeId != null)
            {
                return _deserializeWithNativeTypeId(jp, ctxt, typeId);
            }
        }
        String typeId = _locateTypeId(jp, ctxt);
        JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
        if (_typeIdVisible && !_usesExternalId() && jp.getCurrentToken() == JsonToken.START_OBJECT)
        {
            TokenBuffer tb = new TokenBuffer(null, false);
            tb.writeStartObject();
            tb.writeFieldName(_typePropertyName);
            tb.writeString(typeId);
            jp = JsonParserSequence.createFlattened(tb.asParser(jp), jp);
            jp.nextToken();
        }
        Object value = deser.deserialize(jp, ctxt);
        return value;
    }
}

class ClassIdAsPropertyTypeSerializer extends AsPropertyTypeSerializer
{
    public ClassIdAsPropertyTypeSerializer(final TypeIdResolver idRes, final String _typeProperty)
    {
        super(idRes, null, _typeProperty);
    }

    @Override
    public void writeTypePrefixForArray(final Object value, final JsonGenerator jgen) throws IOException
    {
        jgen.writeStartArray();
    }

    @Override
    public void writeTypeSuffixForArray(final Object value, final JsonGenerator jgen) throws IOException
    {
        jgen.writeEndArray();
    }
}
