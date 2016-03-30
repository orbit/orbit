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

package cloud.orbit.actors.test.stateless;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeSync;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class StatelessWithTimersTest extends ActorBaseTest
{
    @StatelessWorker
    public interface StatelessTimed extends Actor
    {
        Task<String> scheduleSomething(String msg);
    }

    public static class StatelessTimedActor extends AbstractActor implements StatelessTimed
    {
        @Inject
        private FakeSync sync;

        @Override
        public Task<String> scheduleSomething(String msg)
        {
            registerTimer(() -> {
                sync.task("name").complete(msg);
                return Task.done();
            }, 100, TimeUnit.MILLISECONDS);
            return Task.fromValue(msg);
        }

        @Override
        public Task<?> deactivateAsync()
        {
            sync.semaphore("deactivation").release();
            return super.deactivateAsync();
        }
    }

    @Test
    public void scheduleSimpleTimer() throws ExecutionException, InterruptedException
    {
        createStage();
        StatelessTimed actor1 = Actor.getReference(StatelessTimed.class, "1000");
        assertEquals("hello", actor1.scheduleSomething("hello").join());
        // this will be released when the timer is completed
        fakeSync.task("name").join();
    }

    @Test
    public void scheduleSimpleTimerAndStopStage() throws ExecutionException, InterruptedException
    {
        final Stage stage1 = createStage();
        StatelessTimed actor1 = Actor.getReference(StatelessTimed.class, "1000");
        assertEquals("hello", actor1.scheduleSomething("hello").join());
        // this will be released when the timer is completed
        fakeSync.task("name").join();
        stage1.stop().join();
    }

}
