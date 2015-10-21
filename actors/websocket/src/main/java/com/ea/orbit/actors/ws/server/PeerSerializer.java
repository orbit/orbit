package com.ea.orbit.actors.ws.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface PeerSerializer
{
    Message deserializeMessage(final com.ea.orbit.actors.runtime.Runtime runtime, final InputStream inputStream) throws Exception;

    void serializeMessage(final com.ea.orbit.actors.runtime.Runtime runtime, OutputStream out, Message message) throws Exception;

    Object convertValue(Object o, Type genericParameterType);
}
