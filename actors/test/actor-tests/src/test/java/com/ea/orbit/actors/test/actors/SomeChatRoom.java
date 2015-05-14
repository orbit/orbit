package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.Task;

public interface SomeChatRoom extends Actor
{
    Task<Void> join(SomeChatObserver chatObserver);

    Task<Void> sendMessage(SomeChatObserver chatObserver, String message);

    Task<?> startCountdown(int count, String message);

}
