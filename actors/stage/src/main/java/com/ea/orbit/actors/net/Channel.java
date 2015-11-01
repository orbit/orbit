package com.ea.orbit.actors.net;

import com.ea.orbit.actors.runtime.Message;

public interface Channel
{
    void sendMessage(Message message);
    void setMessageListener(MessageListener listener);
}
