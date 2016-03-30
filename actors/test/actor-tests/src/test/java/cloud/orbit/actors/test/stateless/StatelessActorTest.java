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
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.actors.StatelessThing;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatelessActorTest extends ActorBaseTest
{

    @Test
    public void simpleStatelessTest() throws ExecutionException, InterruptedException
    {
        createStage();
        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        assertNotNull(actor1.getUniqueActivationId().join());
        dumpMessages();
    }

    @Test
    public void simpleStatelessTest2() throws ExecutionException, InterruptedException
    {
        createStage();
        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        for (int i = 0; i < 100; i++)
        {
            assertNotNull(actor1.sayHello().join());
        }
    }

    @Test
    public void simpleStatelessTestConcurrentCalls() throws ExecutionException, InterruptedException, TimeoutException
    {
        createStage();
        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        assertNotNull(actor1.sayHello().join());

        List<Task<String>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++)
        {
            futures.add(actor1.sayHello());
        }
        Task.allOf(futures).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void simpleStatelessTwoStages() throws ExecutionException, InterruptedException, TimeoutException
    {
        final Stage stage1 = createStage();
        StatelessThing actor = Actor.getReference(StatelessThing.class, "1000");
        createStage();
        assertNotNull(actor.sayHello().get(10, TimeUnit.SECONDS));
        stage1.bind();
        assertNotNull(actor.sayHello().get(10, TimeUnit.SECONDS));
        Thread.sleep(500);
        dumpMessages();
    }


    @Test
    public void simpleStatelessThreeStages() throws ExecutionException, InterruptedException, TimeoutException
    {
        final Stage stage1 = createStage();
        StatelessThing actor = Actor.getReference(StatelessThing.class, "1000");
        stage1.bind();
        assertNotNull(actor.sayHello().get(10, TimeUnit.SECONDS));
        final Stage stage2 = createStage();
        stage2.bind();
        assertNotNull(actor.sayHello().get(10, TimeUnit.SECONDS));
        final Stage stage3 = createStage();
        stage1.stop().join();
        stage2.stop().join();
        stage3.bind();
        assertNotNull(actor.sayHello().get(10, TimeUnit.SECONDS));
        Thread.sleep(500);
        stage3.stop().join();
        dumpMessages();
    }
    @Test
    public void simpleStatelessTestConcurrentTwoStages() throws ExecutionException, InterruptedException, TimeoutException
    {
        final Stage stage1 = createStage();
        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        final Stage stage2 = createStage();
        StatelessThing actor2 = Actor.getReference(StatelessThing.class, "1000");
        assertNotNull(actor1.sayHello().join());

        List<Task<String>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stage1.bind();
            futures.add(actor1.sayHello());
            stage2.bind();
            futures.add(actor2.sayHello());
        }
        Task.allOf(futures).get(30, TimeUnit.SECONDS);
    }


    @Test
    public void statelessTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        Stage stage3 = createStage();
        Stage stage4 = createStage();
        Stage client = createClient();

        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        StatelessThing actor2 = Actor.getReference(StatelessThing.class, "1000");
        StatelessThing actor3 = Actor.getReference(StatelessThing.class, "1000");
        StatelessThing actor4 = Actor.getReference(StatelessThing.class, "1000");
        StatelessThing actor5 = Actor.getReference(StatelessThing.class, "1000");

        final Set<UUID> set = new HashSet<>();
        Supplier stagesIdle = () -> Stream.of(stage1, stage2, stage3, stage4).allMatch(s -> isIdle(s));
        for (int i = 0; i < 25; i++)
        {
            // there will be no concurrent executions here
            // each node will have at most one activation of the stateless worker
            stage1.bind();
            waitFor(stagesIdle);
            set.add(actor1.getUniqueActivationId().join());
            stage2.bind();
            waitFor(stagesIdle);
            set.add(actor2.getUniqueActivationId().join());
            stage3.bind();
            waitFor(stagesIdle);
            set.add(actor3.getUniqueActivationId().join());
            stage4.bind();
            waitFor(stagesIdle);
            set.add(actor4.getUniqueActivationId().join());
            client.bind();
            waitFor(stagesIdle);
            set.add(actor5.getUniqueActivationId().join());
        }
        // Statistics might let us down from time to time here...

        // Also it might just happen that between the server sending the response and releasing the activation,
        // the client might have enough time to send another messages causing a new activation.
        // The problem here is that the fake network is too fast to test this properly
        // This could be "fixed" with some sleeps.

        // We are using " awaitFor(stagesIdle)" to ensure no stages are processing any messages,
        // but it's not bullet proof yet.
        // ideally I'd like to test for this:
        // assertEquals(4, set.size());
        // however since awaitFor(stagesIdle) is not a sure thing, I must be content with:
        final int size1 = set.size();
        assertTrue("Expecting <=" + 16 + " but was: " + size1, size1 <= 16);
        // TODO: find a way to know for sure, from the outside, that the stage is free to execute a new call.


        set.clear();
        List<Future<UUID>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++)
        {
            // this will force the creation of concurrent activations in each node
            stage1.bind();
            futures.add(actor1.getUniqueActivationId(5000));
            stage2.bind();
            futures.add(actor2.getUniqueActivationId(5000));
            stage3.bind();
            futures.add(actor3.getUniqueActivationId(5000));
            stage4.bind();
            futures.add(actor4.getUniqueActivationId(5000));
            client.bind();
            futures.add(actor5.getUniqueActivationId(5000));
        }
        futures.forEach(f -> {
            try
            {
                set.add(f.get(10, TimeUnit.SECONDS));
            }
            catch (Exception e)
            {
                throw new UncheckedException(e);
            }
        });
        // it is very likely that there will be more than one activation per stage host.
        final int size = set.size();
        assertTrue("Expecting >4 but was: " + size, size > 4);
        // only 25*5 calls => there should not be more than 125 activations
        assertTrue("Expecting <=" + (50 * 5) + " but was: " + size, size <= 50 * 5);
    }

    /**
     * Sends a bit more messages trying to uncover concurrency issues.
     */
    @Test
    public void heavierTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();

        StatelessThing actor1 = Actor.getReference(StatelessThing.class, "1000");
        StatelessThing actor2 = Actor.getReference(StatelessThing.class, "1000");

        final Set<UUID> set = new HashSet<>();

        set.clear();
        List<Future<UUID>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++)
        {
            // this will force the creation of concurrent activations in each node
            stage1.bind();
            futures.add(actor1.getUniqueActivationId());
            stage2.bind();
            futures.add(actor2.getUniqueActivationId());
        }
        futures.forEach(f -> {
            try
            {
                set.add(f.get(10, TimeUnit.SECONDS));
            }
            catch (Exception e)
            {
                throw new UncheckedException(e);
            }
        });
        // it is very likely that there will be more than one activation per stage host.
        assertTrue(set.size() > 1);
        // only 25*5 calls => there should not be more than 125 activations
        assertTrue(set.size() <= 100);
    }

}
