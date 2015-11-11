package com.ea.orbit.actors.extensions;

import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.Message;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Extension interface to define how actor messages are serialized.
 */
public interface MessageSerializer extends ActorExtension
{
    Message deserializeMessage(final BasicRuntime runtime, final InputStream inputStream) throws Exception;

    void serializeMessage(final BasicRuntime runtime, OutputStream out, Message message) throws Exception;
}
