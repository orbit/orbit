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

package cloud.orbit.actors.test;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.test.actors.SomeActor;
import cloud.orbit.concurrent.Task;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        Assert.assertEquals(uuid, someActor.getUniqueActivationId().get());
        Task<UUID> call = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));
        clock.incrementTime(60, TimeUnit.MINUTES);
        client.cleanup();
        expectException(() -> call.join());
    }

    @Test
    public void timeoutAnnotationTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage client = createClient();

        SomeActor someActor = Actor.getReference(SomeActor.class, "1");

        UUID uuid = someActor.getUniqueActivationId(0).get();
        Assert.assertEquals(uuid, someActor.getUniqueActivationId().get());
        Task<UUID> call = someActor.getUniqueActivationIdWithTimeoutAnnotation(TimeUnit.SECONDS.toNanos(200));
        clock.incrementTime(5, TimeUnit.SECONDS);
        client.cleanup();
        expectException(() -> call.join());
    }

    @Test
    public void timeoutWithTwoMessagesTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage client = createClient();

        SomeActor someActor = Actor.getReference(SomeActor.class, "1");

        UUID uuid = someActor.getUniqueActivationId(0).get();
        Assert.assertEquals(uuid, someActor.getUniqueActivationId().get());

        // first
        Task<UUID> first = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));
        // speeding up the time.
        clock.incrementTime(60, TimeUnit.MINUTES);
        // later call
        Task<UUID> second = someActor.getUniqueActivationId(TimeUnit.SECONDS.toNanos(200));

        // true only because the cleanup has not been called yet.
        assertFalse(first.isDone());
        // the second call is still seconds away from the timeout
        assertFalse(second.isDone());

        // now the first call will timeout because the time has passed and cleanup is being called
        client.cleanup().join();

        // after the cleanup this call is a goner.
        eventuallyTrue(() -> first.isCompletedExceptionally());

        // second call is still good
        assertFalse(second.isDone());
        // second call is still good
        // however if the time speeds up
        clock.incrementTime(60, TimeUnit.MINUTES);
        assertFalse(second.isDone());
        // and cleanup runs
        client.cleanup();
        // the second call also times out
        eventuallyTrue(() -> second.isCompletedExceptionally());

        expectException(() -> first.join());
        expectException(() -> second.join());
    }

    @Test
    public void timeoutWithoutCallingCleanup()
    {
        clock.stop();
        Stage stage1 = createStage();

        SomeActor actor = Actor.getReference(SomeActor.class, "1");

        UUID uuid = actor.getUniqueActivationId(0).join();
        Assert.assertEquals(uuid, actor.getUniqueActivationId(0).join());

        final Task<UUID> timeoutCall = actor.getUniqueActivationId(TimeUnit.MINUTES.toNanos(2));
        clock.incrementTime(60, TimeUnit.MINUTES);
        // not calling stage.cleanup, to make sure the timeout cleanup is being called
        eventuallyTrue(() -> timeoutCall.isCompletedExceptionally());
    }

}
