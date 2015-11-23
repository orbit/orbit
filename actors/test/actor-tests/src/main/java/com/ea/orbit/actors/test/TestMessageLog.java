package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.PipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.RemoteReference;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


class TestMessageLog implements PipelineExtension
{
    private AtomicLong invocationId = new AtomicLong();

    private ActorBaseTest actorBaseTest;
    private Stage stage;

    public TestMessageLog(final ActorBaseTest actorBaseTest, final Stage stage)
    {
        this.actorBaseTest = actorBaseTest;
        this.stage = stage;
    }

    @Override
    public String getName()
    {
        return "test-message-logging";
    }

    @Override
    public String afterHandlerName()
    {
        return DefaultHandlers.MESSAGING;
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

    @Override
    public void onRead(HandlerContext ctx, Object msg)
    {
        if (!(msg instanceof Message))
        {
            ctx.fireRead(msg);
            return;
        }
        final Message message = (Message) msg;
        long messageId = message.getMessageId();


        String from = message.getFromNode() != null ? String.valueOf(message.getFromNode().asUUID().getLeastSignificantBits()) : "QQQ";
        String to = message.getToNode() != null ? String.valueOf(message.getToNode().asUUID().getLeastSignificantBits())
                : String.valueOf(stage.getHosting().getNodeAddress().asUUID().getLeastSignificantBits());

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
        final String seqMsg = '"' + from + "\" -> \"" + to + "\" : [" + messageId + "] " + strTarget + " " + strParams;
        actorBaseTest.sequenceDiagram.add(seqMsg);
        while (actorBaseTest.sequenceDiagram.size() > 100)
        {
            actorBaseTest.sequenceDiagram.remove(0);
        }
        actorBaseTest.hiddenLog.info(seqMsg);

        ctx.fireRead(msg);
    }


}
