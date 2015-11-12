package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.DefaultClassDictionary;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

class ServerPeerExecutor extends HandlerAdapter
{
    private static Logger logger = LoggerFactory.getLogger(ServerPeerExecutor.class);
    private final Stage stage;

    public ServerPeerExecutor(final Stage stage)
    {
        this.stage = stage;
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        // TODO: server-to-client invocation ?
        return super.write(ctx, msg);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        if (msg instanceof Message)
        {
            final Message message = (Message) msg;
            final int messageType = message.getMessageType();
            final NodeAddress fromNode = message.getFromNode();
            final int messageId = message.getMessageId();
            switch (messageType)
            {
                case MessageDefinitions.ONE_WAY_MESSAGE:
                case MessageDefinitions.REQUEST_MESSAGE:
                    final Class<?> clazz = DefaultClassDictionary.get().getClassById(message.getInterfaceId());
                    final Addressable reference = (Addressable)stage.getReference((Class)clazz, message.getObjectId());
                    final boolean oneWay = message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE;
                    final Method method = stage.getInvoker(message.getInterfaceId()).getMethod(message.getMethodId());
                    final Task<?> res = stage.invoke(reference, method,
                            oneWay, message.getMethodId(),
                            (Object[]) message.getPayload());
                    res.whenComplete((r, e) ->
                            sendResponseAndLogError(ctx,
                                    messageType == MessageDefinitions.ONE_WAY_MESSAGE,
                                    fromNode, messageId, r, e));
                    break;
            }
        }
    }


    protected void sendResponseAndLogError(HandlerContext ctx, boolean oneway, final NodeAddress from, int messageId, Object result, Throwable exception)
    {
        if (exception != null && logger.isDebugEnabled())
        {
            logger.debug("Unknown application error. ", exception);
        }

        if (!oneway)
        {
            if (exception == null)
            {
                sendResponse(ctx, from, MessageDefinitions.RESPONSE_OK, messageId, result);
            }
            else
            {
                sendResponse(ctx, from, MessageDefinitions.RESPONSE_ERROR, messageId, exception);
            }
        }
    }

    private Task sendResponse(HandlerContext ctx, NodeAddress to, int messageType, int messageId, Object res)
    {
        return ctx.write(new Message()
                .withToNode(to)
                .withMessageId(messageId)
                .withMessageType(messageType)
                .withPayload(res));
    }
}
