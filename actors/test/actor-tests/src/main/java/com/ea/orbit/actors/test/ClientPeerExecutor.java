package com.ea.orbit.actors.test;

import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClientPeerExecutor extends HandlerAdapter
{
    private static Logger logger = LoggerFactory.getLogger(ClientPeerExecutor.class);

    public ClientPeerExecutor()
    {

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
                    // todo
            }
        }
    }

}
