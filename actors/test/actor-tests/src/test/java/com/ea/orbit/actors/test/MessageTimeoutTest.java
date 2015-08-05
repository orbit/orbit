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

package com.ea.orbit.actors.test;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.test.actors.SomeActor;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class MessageTimeoutTest extends ActorBaseTest
{

    @Test
    public void timeoutTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage client = createClient();

        SomeActor someActor = Actor.getReference(SomeActor.class, "1");

        UUID uuid = someActor.getUniqueActivationId(0).get();
        assertEquals(uuid, someActor.getUniqueActivationId().get());
        Future<UUID> call = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(60));
        client.cleanup(false);
        assertTrue(call.isDone());
        expectException(() -> call.get());
    }

    @Test
    public void timeoutWithTwoMessagesTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage client = createClient();

        SomeActor someActor = Actor.getReference(SomeActor.class, "1");

        UUID uuid = someActor.getUniqueActivationId(0).get();
        assertEquals(uuid, someActor.getUniqueActivationId().get());

        // first
        Future<UUID> first = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));
        // speeding up the time.
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(60));
        // later call
        Future<UUID> second = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));

        // true only because the cleanup has not been called yet.
        assertFalse(first.isDone());
        // the second call is still seconds away from the timeout
        assertFalse(second.isDone());

        // now the first call will timeout because the time has passed and cleanup is being called
        client.cleanup(false);

        // after the cleanup this call is a goner.
        assertTrue(first.isDone());

        // second call is still good
        assertFalse(second.isDone());
        // second call is still good
        // however if the time speeds up
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(60));
        assertFalse(second.isDone());
        // and cleanup runs
        client.cleanup(false);
        // the second call also times out
        assertTrue(second.isDone());

        expectException(() -> first.get());
        expectException(() -> second.get());
    }

}
