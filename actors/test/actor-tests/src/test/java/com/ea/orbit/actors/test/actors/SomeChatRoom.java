package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

public interface SomeChatRoom extends IActor
{
    Task<Void> join(ISomeChatObserver chatObserver);

    Task<Void> sendMessage(ISomeChatObserver chatObserver, String message);

    Task<?> startCountdown(int count, String message);

}
