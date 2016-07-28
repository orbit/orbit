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

package cloud.orbit.actors.test;

import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.DefaultClassDictionary;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.InternalUtils;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;
import cloud.orbit.tuples.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;


public class TestMessageLog extends NamedPipelineExtension
{
    private TestLogger logger;
    private String name;

    public TestMessageLog(TestLogger logger)
    {
        this(logger, "test-message-logging", null, DefaultHandlers.MESSAGING);
    }

    public TestMessageLog(TestLogger logger, final String name)
    {
        this(logger, name, null, DefaultHandlers.MESSAGING);
    }

    public TestMessageLog(TestLogger logger, final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
        this.logger = logger;
        this.name = name;
    }


    @Override
    public void onRead(HandlerContext ctx, Object msg)
    {
        if ((msg instanceof Message))
        {
            logMessage((Message) msg, true);
        }
        else if ((msg instanceof Pair))
        {
            //noinspection unchecked
            logBytes((Pair<Object, byte[]>) msg);
        }
        ctx.fireRead(msg);
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if ((msg instanceof Message))
        {
            logMessage((Message) msg, false);
        }
        else if ((msg instanceof Pair))
        {
            //noinspection unchecked
            logBytes((Pair<Object, byte[]>) msg);
        }

        return ctx.write(msg);
    }

    private void logBytes(Pair<Object, byte[]> pair)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String buffer = new String(pair.getRight(), 4, pair.getRight().length - 4, StandardCharsets.UTF_8);
        logger.write(buffer);

        try
        {
            logger.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(buffer, Object.class)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        logger.write(InternalUtils.hexDump(32, pair.getRight(), 0, pair.getRight().length));
    }

    private void logMessage(final Message message, boolean in)
    {
        long messageId = message.getMessageId();


        String from = message.getFromNode() != null ? String.valueOf(message.getFromNode().asUUID().getLeastSignificantBits()) : in ? "IN" : "OUT";
        String to = message.getToNode() != null ? String.valueOf(message.getToNode().asUUID().getLeastSignificantBits())
                : in ? "OUT" : "IN";

        String strParams = "";
        final Object payload = message.getPayload();
        if (payload instanceof Object[])
        {
            final Object[] params = (Object[]) payload;
            if (params.length > 0)
            {
                try
                {
                    strParams = Arrays.asList(params).stream().map(a -> toString(a)).collect(Collectors.joining(", ", "(", ")"));
                }
                catch (Exception ex)
                {
                    strParams = "(can't show parameters)";
                }
            }
        }
        else
        {
            strParams = payload != null ? String.valueOf(payload) : "";
        }
        String strTarget = "";
        if (message.getInterfaceId() != 0)
        {
            Class clazz = DefaultClassDictionary.get().getClassById(message.getInterfaceId());
            if (clazz != null)
            {
                final Method method = DefaultDescriptorFactory.get().getInvoker(clazz).getMethod(message.getMethodId());
                strTarget = clazz.getSimpleName() + ":" + message.getObjectId() + "." + method.getName();
            }
        }
        final String seqMsg = '"' + from + "\" -> \"" + to + "\" : [" + name + ":" + messageId + "] " + strTarget + " " + strParams;
        logger.sequenceDiagram.add(seqMsg);
        while (logger.sequenceDiagram.size() > 100)
        {
            logger.sequenceDiagram.remove(0);
        }
        logger.write(seqMsg);
    }

    String toString(Object obj)
    {
        if (obj instanceof String)
        {
            return (String) obj;
        }
        if (obj instanceof AbstractActor)
        {
            final RemoteReference ref = RemoteReference.from((AbstractActor) obj);
            return RemoteReference.getInterfaceClass(ref).getSimpleName() + ":" +
                    RemoteReference.getId(ref);
        }
        if (obj instanceof RemoteReference)
        {
            return RemoteReference.getInterfaceClass((RemoteReference<?>) obj).getSimpleName() + ":" +
                    RemoteReference.getId((RemoteReference<?>) obj);
        }
        return String.valueOf(obj);
    }

}
