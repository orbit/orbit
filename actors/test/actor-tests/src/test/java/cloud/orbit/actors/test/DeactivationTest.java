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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.test.actors.SomeActor;
import cloud.orbit.actors.test.actors.StatelessThing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class DeactivationTest extends ClientTest
{
    @Test
    public void simpleCleanupTest() throws ExecutionException, InterruptedException
    {
        loggerExtension.enableDebugFor(Stage.class);
        clock.stop();
        Stage stage = createStage();
        SomeActor actor1 = Actor.getReference(SomeActor.class, "1000");

        final UUID id = actor1.getUniqueActivationId().join();
        clock.incrementTime(20, TimeUnit.MINUTES);
        stage.cleanup().join();
        Assert.assertNotEquals(id, actor1.getUniqueActivationId().join());
        dumpMessages();
    }

    @SuppressWarnings("unused")
    @Test(timeout = 15_000L)
    public void simpleStatelessWorkerDeactivationTest() throws ExecutionException, InterruptedException, TimeoutException
    {
        clock.stop();
        final Stage stage = createStage();

        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        final UUID id = actor1.getUniqueActivationId().join();
        clock.incrementTime(20, TimeUnit.MINUTES);
        stage.cleanup().join();
        Assert.assertNotEquals(id, actor1.getUniqueActivationId().join());
        dumpMessages();
    }

    @Test
    public void cleanupTest() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage = createStage();
        Stage client = createClient();

        SomeActor actor1 = Actor.getReference(SomeActor.class, "1000");

        final Set<UUID> set = new HashSet<>();
        client.bind();
        for (int i = 0; i < 25; i++)
        {
            set.add(actor1.getUniqueActivationId().join());
        }
        assertEquals(1, set.size());

        // shouldn't collect anything, since clock is moving slowly
        stage.cleanup().join();
        client.bind();
        set.add(actor1.getUniqueActivationId().join());
        assertEquals(1, set.size());

        waitFor(() -> isIdle(stage));
        clock.incrementTime(20, TimeUnit.MINUTES);
        stage.cleanup().join();
        client.bind();
        set.add(actor1.getUniqueActivationId().join());
        assertEquals(2, set.size());
    }

    @Test
    public void dirtyLocalCache() throws ExecutionException, InterruptedException, TimeoutException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        SomeActor actor1 = Actor.getReference(SomeActor.class, "1");

        stage1.bind();
        actor1.sayHello("huuhaa").join();
        stage2.bind();
        assertNotNull(actor1.sayHelloOnlyIfActivated().join());
        clock.incrementTimeMillis(TimeUnit.HOURS.toMillis(1));
        stage1.cleanup().join();
        eventuallyTrue(6000, () -> stage1.locateActor(RemoteReference.from(actor1), false).get() == null);
        assertNull(actor1.sayHelloOnlyIfActivated().join());
        dumpMessages();
    }

    @SuppressWarnings("unused")
    @Test
    @Ignore
    public void statelessWorkerDeactivationTestWithLeftover() throws ExecutionException, InterruptedException, TimeoutException
    {
        clock.stop();
        Stage stage1 = createStage();

        StatelessThing actor5 = Actor.getReference(StatelessThing.class, "1000");

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
        waitFor(() -> isIdle(stage1));
        clock.incrementTime(8, TimeUnit.MINUTES);

        // touch a single activation (that will probably not be collected)
        UUID theSurviving = actor5.getUniqueActivationId().get();

        waitFor(() -> isIdle(stage1));
        clock.incrementTime(8, TimeUnit.MINUTES);


        // THE CLEANUP
        // this will collect all but one activation
        loggerExtension.enableDebugFor(Stage.class);
        waitFor(() -> isIdle(stage1));
        stage1.cleanup().join();
        waitFor(() -> isIdle(stage1));
        stage1.cleanup().join();

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
        // the cleanup might be partial because "isIdle" it not precise.
        assertTrue("setSize: " + set2.size(), 5 > set2.size());
    }

    @SuppressWarnings("unused")
    @Test(timeout = 15_000L)
    public void statelessWorkerDeactivationTest() throws ExecutionException, InterruptedException, TimeoutException
    {
        clock.stop();
        Stage stage1 = createStage();

        StatelessThing actor5 = Actor.getReference(StatelessThing.class, "1000");

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
        waitFor(() -> isIdle(stage1));
        clock.incrementTime(20, TimeUnit.MINUTES);

        waitFor(() -> isIdle(stage1));


        // THE CLEANUP
        // this will collect all but one activation
        loggerExtension.enableDebugFor(Stage.class);
        stage1.cleanup().join();
        waitFor(() -> isIdle(stage1));
        stage1.cleanup().join();

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

        // and no other ids will match
        set2.retainAll(set1);
        // the cleanup might be partial because "isIdle" it not precise.
        assertEquals("setSize: ", 0, set2.size());
    }
}
