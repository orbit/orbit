package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.IActorObserver;
import com.ea.orbit.concurrent.Task;

public interface ISomeChatObserver extends IActorObserver
{
    Task<Void> receiveMessage(final ISomeChatObserver sender, String message);
}
