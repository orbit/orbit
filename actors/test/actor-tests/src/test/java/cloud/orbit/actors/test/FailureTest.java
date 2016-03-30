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

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FailureTest extends ActorBaseTest
{
    String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();

    @Test
    public void nodeDropTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();

        SomeActor someActor = Actor.getReference(SomeActor.class, "1");
        stage1.bind();
        UUID uuid = someActor.getUniqueActivationId().join();
        assertEquals("bla", someActor.sayHello("bla").join());

        Stage stage3 = createStage();
        Stage stage4 = createStage();


        SomeActor someActor_r3 = Actor.getReference(SomeActor.class, "1");
        stage3.bind();
        assertEquals(uuid, someActor_r3.getUniqueActivationId().join());

        stage1.stop().join();
        stage2.stop().join();

        // a new Activation must have been created since the initial nodes where stopped.
        stage3.bind();
        final UUID secondUUID = someActor_r3.getUniqueActivationId().join();
        assertNotEquals(uuid, secondUUID);
        SomeActor someActor_r4 = Actor.getReference(SomeActor.class, "1");
        stage3.bind();
        assertEquals(secondUUID, someActor_r4.getUniqueActivationId().join());
        // BTW, timing issues will sometimes make this fail by timeout with the real network.
        stage3.stop().join();
        stage4.stop().join();

    }

}
