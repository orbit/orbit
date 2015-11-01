package com.ea.orbit.actors.net;

import com.ea.orbit.actors.runtime.Message;

@FunctionalInterface
public interface MessageListener
{
    void recvMessage(Message message);
}
