package com.ea.orbit.actors.net;

import com.ea.orbit.concurrent.Task;

// netty inspired channel processors
public interface Channel
{
    /**
     * Not thread safe, should be called before starting the channel
     * The first Handler added is the first outbound handler, the first to handle a write, and the last inbound
     * The last Handler added is the first inbound handler, the first to receive a message, and the last outbound
     */
    void addHandler(ChannelHandler handler);

    Task<Void> write(Object message);

    Task<Void> connect(Object param);

    Task<Void> disconnect();
}
