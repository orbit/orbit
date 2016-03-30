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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class MinimalTest extends ActorBaseTest
{
    String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();

    @Test
    public void singleActorSingleStageTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeActor someActor = Actor.getReference(SomeActor.class, "1");
        assertEquals("bla", someActor.sayHello("bla").get());
    }


    @Test
    public void singeActor2StageTest() throws ExecutionException, InterruptedException
    {
        for (int i = 0; i < 2; i++)
        {
            Stage stage = createStage();
            SomeActor someActor = Actor.getReference(SomeActor.class, "1");
            assertEquals("bla", someActor.sayHello("bla").get());
        }
    }

    @Test
    public void noActor2StageTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();
    }

    @Test
    public void singleActorMultiStageTest() throws ExecutionException, InterruptedException
    {
        for (int i = 0; i < 10; i++)
        {
            Stage stage = createStage();
            SomeActor someActor = Actor.getReference(SomeActor.class, "1");
            assertEquals("bla", someActor.sayHello("bla").get());
        }
    }

    @Test
    public void multipleActorsOfTheSameType() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }

        Random r = new Random();
        for (int i = 0; i < 100; i++)
        {
            stages.get(r.nextInt(stages.size())).bind();
            SomeActor actor = Actor.getReference(SomeActor.class, String.valueOf(i));
            assertEquals("bla", actor.sayHello("bla").join());
        }
    }

    @Test
    public void ensureUniqueActivation() throws ExecutionException, InterruptedException
    {
        Stage stage0 = createStage();
        UUID uuid = Actor.getReference(SomeActor.class, "1").getUniqueActivationId().get();
        for (int i = 0; i < 10; i++)
        {
            Stage stage = createStage();
            SomeActor someActor = Actor.getReference(SomeActor.class, "1");
            assertEquals("bla", someActor.sayHello("bla").get());
            assertEquals(uuid, someActor.getUniqueActivationId().get());
        }
    }

}
