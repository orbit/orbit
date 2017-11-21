/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.Deactivate;
import cloud.orbit.actors.extensions.ActorCountDeactivationExtension;
import cloud.orbit.actors.extensions.test.ConstructionTest;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StageTest
{
    @Test
    public void testActivateActor() throws Exception
    {
        final Stage stage = new Stage.Builder()
                .build();

        try {

            stage.start().join();

            long initialLocalObjects = stage.getLocalObjectCount();

            // activate an actor
            TestStage testActor = Actor.getReference(TestStage.class, "0");

            // check actor is in local objects
            assertEquals(initialLocalObjects+1, stage.getLocalObjectCount());

            // assert activateAsync has been called
            assertTrue(testActor.isActivated().get());
        } finally {
            stage.stop().join();
        }
    }

    @Test
    public void testDeactivateActor() throws Exception
    {
        final Stage stage = new Stage.Builder()
                .build();

        try {

            stage.start().join();


            // activate an actor
            TestStage testActor = Actor.getReference(TestStage.class, "0");

            // assert activateAsync has been called
            assertTrue(testActor.isActivated().get());
            // assert deactivateAsync has not been called
            assertFalse(testActor.isDeactivated().get());

            long initialLocalObjects = stage.getLocalObjectCount();

            testActor.deactivate().get();

            // check actor is in local objects
            assertEquals(initialLocalObjects-1, stage.getLocalObjectCount());
        } finally {
            stage.stop().join();
        }
    }

    public interface TestStage extends Actor
    {
        public Task<Boolean> isActivated();
        public Task<Boolean> isDeactivated();

        @Deactivate
        public Task<Void> deactivate();
    }

    public static class TestStageActor extends AbstractActor implements TestStage
    {
        private boolean activated = false;
        private boolean deactivated = false;

        @Override
        public Task<?> deactivateAsync()
        {
            this.deactivated = true;
            return Task.done();
        }

        @Override
        public Task<?> activateAsync()
        {
            this.activated=true;
            return Task.done();
        }

        @Override
        public Task<Boolean> isActivated()
        {
            return Task.fromValue(activated);
        }

        @Override
        public Task<Boolean> isDeactivated()
        {
            return Task.fromValue(deactivated);
        }
    }
}
