package com.ea.orbit.actors.extensions.json;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.actors.runtime.ObjectInvoker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
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

        // todo
//        TypeResolverBuilder<?> typer = new ClassIdTypeResolverBuilder(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
//        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
//        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
//        mapper.setDefaultTyping(typer);
    }


    @Override
    public Message deserializeMessage(final BasicRuntime runtime, final InputStream inputStream) throws Exception
    {
        ensureInit(runtime);
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
        return mapper.convertValue(o, mapper.constructType(genericParameterType));
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

    @Deprecated // since 2.3
    @Override
    public JavaType typeFromId(String id)
    {
        return _typeFromId(id, _typeFactory);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id)
    {
        return _typeFromId(id, context.getTypeFactory());
    }

    @Override
    protected JavaType _typeFromId(final String id, final TypeFactory typeFactory)
    {
        Class<?> cls = DefaultClassDictionary.get().getClassById(Integer.parseInt(id));
        return typeFactory.constructSpecializedType(_baseType, cls);
    }
}
