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


import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.test.actors.ISomeActor;
import com.ea.orbit.actors.test.actors.ISomeMatch;
import com.ea.orbit.actors.test.actors.ISomePlayer;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LifeCycleTest extends ActorBaseTest
{
    @Test
    public void activationTest() throws ExecutionException, InterruptedException
    {
        OrbitStage stage = createStage();
        OrbitStage client = createClient();

        ISomeActor actor1 = IActor.getReference(ISomeActor.class, "1000");

        assertTrue(actor1.getActivationWasCalled().get());
    }

    @Test
    public void deactivationTest() throws ExecutionException, InterruptedException
    {
        OrbitStage stage = createStage();
        OrbitStage client = createClient();

        ISomeMatch match = IActor.getReference(ISomeMatch.class, "1000");
        ISomePlayer player = IActor.getReference(ISomePlayer.class, "101");
        match.addPlayer(player).get();
        int machEventCount = player.getMatchEventCount().get();

        // moving the time ahead
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(60));

        // touching the player to prevent it's deactivation;
        assertEquals(machEventCount, player.getMatchEventCount().get().intValue());
        stage.cleanup(true);

        // the match calls a player event on it's deactivation
        assertNotEquals(machEventCount, player.getMatchEventCount().get().intValue());
    }

}
