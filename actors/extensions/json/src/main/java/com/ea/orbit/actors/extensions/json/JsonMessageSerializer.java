package com.ea.orbit.actors.extensions.json;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.actors.runtime.ObjectInvoker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
