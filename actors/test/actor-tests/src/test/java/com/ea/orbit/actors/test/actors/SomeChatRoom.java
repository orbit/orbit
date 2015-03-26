package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.actors.runtime.Registration;
import com.ea.orbit.concurrent.Task;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SomeChatRoom extends OrbitActor implements ISomeChatRoom
{

    Set<ISomeChatObserver> observers = new HashSet<>();
    Registration timer;
    AtomicInteger countDown = new AtomicInteger();

    @Override
    public Task<Void> join(final ISomeChatObserver chatObserver)
    {
        observers.add(chatObserver);
        return Task.done();
    }

    @Override
    public Task<Void> sendMessage(final ISomeChatObserver sender, final String message)
    {
        observers.forEach(o -> o.receiveMessage(sender, message));
        return Task.done();
    }

    @Override
    public Task<Void> startCountdown(final int count, final String message)
    {
        countDown.set(count);
        timer = registerTimer(() -> sendCountDown(message), 5, 5, TimeUnit.MILLISECONDS);
        return Task.done();
    }

    private Task sendCountDown(String message)
    {
        final int count = countDown.decrementAndGet();
        if (count < 0)
        {
            timer.dispose();
            timer = null;
        } else
        {
            observers.forEach(o -> o.receiveMessage(null, message + " " + count));
        }
        return Task.done();
    }
}
