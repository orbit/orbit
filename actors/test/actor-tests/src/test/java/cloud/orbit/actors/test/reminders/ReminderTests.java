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

package cloud.orbit.actors.test.reminders;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ReminderControllerActor;
import cloud.orbit.actors.runtime.ShardedReminderController;
import cloud.orbit.actors.runtime.TickStatus;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class ReminderTests extends ActorBaseTest
{
    // At the moment the clock injected to the stage is not used by the timer subsystem.

    public static interface ReminderTest extends Actor, Remindable
    {

        Task<?> addReminder(String name, long start, long period, TimeUnit unit);

        Task<?> removeReminder(String name);

    }

    private static BlockingQueue<String> remindersReceived = new LinkedBlockingQueue<>();

    @SuppressWarnings("rawtypes")
    public static class ReminderTestActor extends AbstractActor implements ReminderTest
    {

        @Override
        public Task<?> receiveReminder(String reminderName, TickStatus status)
        {
            remindersReceived.add(reminderName);
            return Task.done();
        }

        @Override
        public Task<?> addReminder(String name, long start, long period, TimeUnit unit)
        {
            return registerReminder(name, start, period, unit);
        }

        @Override
        public Task<?> removeReminder(String name)
        {
            return unregisterReminder(name);
        }

    }

    @Test
    public void timerTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage frontend = createClient();

        ReminderTest actor = Actor.getReference(ReminderTest.class, "1");
        actor.addReminder("bla", 0, 50, TimeUnit.MILLISECONDS).join();
        assertEquals("bla", remindersReceived.poll(5, TimeUnit.SECONDS));
        assertEquals("bla", remindersReceived.poll(5, TimeUnit.SECONDS));
    }

    @Test
    public void persistedTimerTest() throws ExecutionException, InterruptedException
    {
        loggerExtension.enableDebugFor(ReminderControllerActor.class);
        Stage stage1 = createStage();
        Stage frontend = createClient();

        ReminderTest actor = Actor.getReference(ReminderTest.class, "1");
        frontend.bind();
        actor.addReminder("bla", 0, 20, TimeUnit.MILLISECONDS).join();
        assertEquals("bla", remindersReceived.poll(5, TimeUnit.SECONDS));

        stage1.stop().join();
        remindersReceived.clear();
        assertNull(remindersReceived.poll(5, TimeUnit.MILLISECONDS));
        Stage stage2 = createStage();
        //actor.addReminder("bla", 0, 20, TimeUnit.MILLISECONDS).join();

        assertEquals("bla", remindersReceived.poll(100, TimeUnit.SECONDS));
        stage2.stop().join();
    }

    @Test
    public void shardTest()
    {
        final Stage stage = createStage(builder -> builder.numReminderControllers(2));
        final String reminderName = UUID.randomUUID().toString();

        final String reminderControllerIdentity = stage.getReminderControllerIdentity(reminderName);
        final ShardedReminderController reminderController = Actor.getReference(ShardedReminderController.class, reminderControllerIdentity);

        final List<String> remindersBeforeAdd = reminderController.getReminders().join();
        assertEquals(0, remindersBeforeAdd.size());

        final ReminderTest testActor = Actor.getReference(ReminderTest.class, reminderName);
        testActor.addReminder(reminderName, 1L, 1L, TimeUnit.DAYS).join();

        final List<String> remindersAfterAdd = reminderController.getReminders().join();
        assertEquals(1, remindersAfterAdd.size());
        assertEquals(reminderName, remindersAfterAdd.get(0));

        testActor.removeReminder(reminderName).join();

        final List<String> remindersAfterRemove = reminderController.getReminders().join();
        assertEquals(0, remindersAfterRemove.size());

    }

}
