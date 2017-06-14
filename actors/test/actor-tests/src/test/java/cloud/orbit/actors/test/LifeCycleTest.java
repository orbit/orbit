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
import cloud.orbit.actors.test.actors.SomeMatch;
import cloud.orbit.actors.test.actors.SomePlayer;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class LifeCycleTest extends ActorBaseTest
{
    @Test
    public void activationTest() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Stage client = createClient();

        SomeActor actor1 = Actor.getReference(SomeActor.class, "1000");

        assertTrue(actor1.getActivationWasCalled().get());
    }

    @Test
    public void deactivationTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage = createStage();
        Stage client = createClient();

        SomeMatch match = Actor.getReference(SomeMatch.class, "1000");
        SomePlayer player = Actor.getReference(SomePlayer.class, "101");
        match.addPlayer(player).get();
        int machEventCount = player.getMatchEventCount().get();

        // moving the time ahead
        waitFor(() -> isIdle(stage));
        clock.incrementTime(60, TimeUnit.MINUTES);

        // touching the player to prevent it's deactivation;
        assertEquals(machEventCount, player.getMatchEventCount().get().intValue());
        stage.cleanup().join();


        // the match calls a player event on it's deactivation
        assertNotEquals(machEventCount, player.getMatchEventCount().get().intValue());
    }

}
