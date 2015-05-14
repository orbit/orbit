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
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.test.actors.SomeActor;
import com.ea.orbit.actors.test.actors.StatelessThing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeactivationTest extends ClientTest
{

    @Test
    public void cleanupTest() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Stage client = createClient();

        SomeActor actor1 = IActor.getReference(SomeActor.class, "1000");

        final Set<UUID> set = new HashSet<>();
        client.bind();
        for (int i = 0; i < 25; i++)
        {
            set.add(actor1.getUniqueActivationId().join());
        }
        assertEquals(1, set.size());

        // shouldn't collect anything, since clock is moving slowly
        stage.cleanup(true);
        client.bind();
        set.add(actor1.getUniqueActivationId().join());
        assertEquals(1, set.size());

        awaitFor(() -> isIdle(stage));
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(20));
        stage.cleanup(true);
        client.bind();
        set.add(actor1.getUniqueActivationId().join());
        assertEquals(2, set.size());
    }

    @SuppressWarnings("unused")
    @Test
    public void statelessWorkerDeactivationTest() throws ExecutionException, InterruptedException, TimeoutException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        StatelessThing actor5 = IActor.getReference(StatelessThing.class, "1000");

        final Set<UUID> set1 = new HashSet<>();
        {
            final List<Future<UUID>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++)
            {
                // this will force the creation of concurrent activations in each node
                futures.add(actor5.getUniqueActivationId(5000));
            }
            for (Future<UUID> f : futures)
            {
                set1.add(f.get(10, TimeUnit.SECONDS));
            }
        }
        // there should be multiple activations, off course statistics might let use down here.
        assertTrue(set1.size() > 1);
        assertTrue(set1.size() <= 100);

        // increment the clock
        awaitFor(() -> isIdle(stage1));
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(8));

        // touch a single activation (that will probably not be collected)
        UUID theSurviving = actor5.getUniqueActivationId().get();

        awaitFor(() -> isIdle(stage1));
        clock.incrementTimeMillis(TimeUnit.MINUTES.toMillis(8));


        // THE CLEANUP
        // this will collect all but one activation
        awaitFor(() -> isIdle(stage1));
        stage1.cleanup(true);


        // do the shenanigans again
        final Set<UUID> set2 = new HashSet<>();
        {
            final List<Future<UUID>> futures = new ArrayList<>();
            for (int i = 0; i < 25; i++)
            {
                // this will force the creation of concurrent activations in each node
                futures.add(actor5.getUniqueActivationId(1000));
            }
            for (Future<UUID> f : futures)
            {
                set2.add(f.get(10, TimeUnit.SECONDS));
            }
        }

        // multiple activations, again
        assertTrue(set2.size() > 1);
        assertTrue(set2.size() <= 100);

        // now there should be a single element from the first set1 that still exists here.
        assertTrue(set2.contains(theSurviving));

        // and no other ids will match
        set2.retainAll(set1);
        assertEquals(1, set2.size());
    }

}
