package com.ea.orbit.actors.test;

import com.ea.orbit.actors.extensions.NamedPipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.tuples.Pair;

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


        logger.write(TestUtils.hexDump(32, pair.getRight(), 0, pair.getRight().length));
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
