package com.ea.orbit.actors.extensions;

import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.Runtime;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Extension interface to define how actor messages are serialized.
 */
public interface MessageSerializer extends ActorExtension
{
    Message deserializeMessage(final com.ea.orbit.actors.runtime.Runtime runtime, final InputStream inputStream) throws Exception;

    void serializeMessage(final Runtime runtime, OutputStream out, Message message) throws Exception;
}
