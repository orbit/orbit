package com.ea.orbit.actors.ws.server;

import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.runtime.Runtime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class JsonPeerSerializer implements PeerSerializer
{
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Message deserializeMessage(final com.ea.orbit.actors.runtime.Runtime runtime, final InputStream inputStream) throws Exception
    {
        return mapper.readValue(inputStream, Message.class);
    }

    @Override
    public void serializeMessage(final Runtime runtime, final OutputStream out, final Message message) throws Exception
    {
        mapper.writeValue(out, message);
    }

    @Override
    public Object convertValue(final Object o, final Type genericParameterType)
    {
        return  mapper.convertValue(o, mapper.constructType(genericParameterType));
    }

    {
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }
}
