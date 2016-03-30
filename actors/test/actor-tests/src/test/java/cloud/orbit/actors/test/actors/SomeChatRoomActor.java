/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.test.actors;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.Registration;
import cloud.orbit.concurrent.Task;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
public class SomeChatRoomActor extends AbstractActor implements SomeChatRoom
{

    Set<SomeChatObserver> observers = new HashSet<>();
    Registration timer;
    AtomicInteger countDown = new AtomicInteger();

    @Override
    public Task<Void> join(final SomeChatObserver chatObserver)
    {
        observers.add(chatObserver);
        return Task.done();
    }

    @Override
    public Task<Void> sendMessage(final SomeChatObserver sender, final String message)
    {
        observers.forEach(o -> o.receiveMessage(sender, message));
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Task<Void> startCountdown(final int count, final String message)
    {
        countDown.set(count);
        timer = registerTimer(() -> sendCountDown(message), 5, 5, TimeUnit.MILLISECONDS);
        return Task.done();
    }

    private Task<Void> sendCountDown(String message)
    {
        final int count = countDown.decrementAndGet();
        if (count < 0)
        {
            timer.dispose();
            timer = null;
        }
        else
        {
            observers.forEach(o -> o.receiveMessage(null, message + " " + count));
        }
        return Task.done();
    }
}
