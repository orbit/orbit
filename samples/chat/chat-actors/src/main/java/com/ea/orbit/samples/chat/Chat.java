/*
Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1.  Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
2.  Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
    its contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ea.orbit.samples.chat;

import com.ea.orbit.actors.ObserverManager;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Chat extends OrbitActor<Chat.State> implements IChat
{
    @Config("orbit.chat.maxMessages")
    private int maxMessages = 1000;

    private long lastSave;

    public static class State
    {
        ObserverManager<IChatObserver> observers = new ObserverManager<>();
        LinkedList<ChatMessageDto> history = new LinkedList<>();
    }

    @Override
    public Task<Void> say(final ChatMessageDto message)
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Message received: " + message.getMessage());
        }
        message.setWhen(new Date());
        state().history.add(message);
        trimHistory();
        state().observers.notifyObservers(o -> o.receiveMessage(message));
        if (System.currentTimeMillis() - lastSave > TimeUnit.SECONDS.toMillis(60))
        {
            writeState().join();
        }
        return Task.done();
    }

    @Override
    public Task<Boolean> join(final IChatObserver observer)
    {
        state().observers.addObserver(observer);
        return writeState().thenApply(x -> true);
    }

    @Override
    public Task<List<ChatMessageDto>> getHistory(int messageCount)
    {
        final LinkedList<ChatMessageDto> history = state().history;
        return Task.fromValue(new ArrayList<>(
                history.subList(Math.max(0, history.size() - messageCount), history.size())));
    }

    @Override
    public Task<Boolean> leave(final IChatObserver observer)
    {
        state().observers.removeObserver(observer);
        return writeState().thenApply(x -> true);
    }

    @Override
    public Task<Void> activateAsync()
    {
        return super.activateAsync().thenRun(() ->
        {
            state().observers.cleanup();
        });
    }

    @Override
    protected Task<Void> writeState()
    {
        lastSave = System.currentTimeMillis();
        trimHistory();
        return super.writeState();
    }

    private void trimHistory()
    {
        while (state().history.size() > maxMessages)
        {
            state().history.remove(0);
        }
    }

    @Override
    public Task<?> deactivateAsync()
    {
        return writeState().thenCompose(() -> super.deactivateAsync());
    }
}