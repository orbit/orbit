package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.concurrent.Task;

public interface SomeChatObserver extends ActorObserver
{
    Task<Void> receiveMessage(final SomeChatObserver sender, String message);
}
