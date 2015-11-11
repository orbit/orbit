package com.ea.orbit.actors.extensions.json;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.runtime.ActorInvoker;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.actors.runtime.DefaultReferenceFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public class JsonMessageSerializer implements MessageSerializer
{
    private ObjectMapper mapper = new ObjectMapper();

    public JsonMessageSerializer()
    {
        mapper.registerModule(new ActorReferenceModule(DefaultReferenceFactory.get()));
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
        final Message message = mapper.readValue(inputStream, Message.class);

        // decode payload parameters according to the interface/method
        if (message.getPayload() != null &&
                (message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE
                        || message.getMessageType() == MessageDefinitions.REQUEST_MESSAGE))
        {
            final ActorInvoker invoker = runtime.getInvoker(message.getInterfaceId());
            final Method method = invoker.getMethod(message.getMethodId());

            final Object[] args = castArgs(method.getGenericParameterTypes(), message.getPayload());
            message.setPayload(args);
        }

        return message;
    }

    @Override
    public void serializeMessage(final BasicRuntime runtime, final OutputStream out, final Message message) throws Exception
    {
        mapper.writeValue(out, message);
    }

    private Object[] castArgs(final Type[] genericParameterTypes, final Object payload)
    {
        Object[] args0 = payload instanceof List ? ((List) payload).toArray() : (Object[]) payload;
        Object[] casted = new Object[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++)
        {
            casted[i] = convertValue(args0[0], genericParameterTypes[i]);
        }
        return casted;
    }

    private Object convertValue(final Object o, final Type genericParameterType)
    {
        return mapper.convertValue(o, mapper.constructType(genericParameterType));
    }


}
