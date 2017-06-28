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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.actors.runtime.DefaultResponseCachingExtension;
import cloud.orbit.actors.test.actors.CacheResponse;
import cloud.orbit.actors.test.actors.CacheResponseActor;
import cloud.orbit.exception.UncheckedException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class CacheResponseTest extends ActorBaseTest
{
    private Stage stage;
    private FakeClock clock;

    @Before
    public void setUp() throws Exception
    {
        clock = new FakeClock();
        clock.stop();
    }

    @Test
    public void testCacheDuration()
    {
        DefaultResponseCachingExtension.setClock(clock);

        CacheResponse actor = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());

        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // New access
        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // Cached access

        clock.incrementTimeMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS / 2);
        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // Still a cached access

        // Advance a Duration cycle
        clock.incrementTimeMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS);
        assertEquals((long) 2, (long) actor.getIndexTally(1).join()); // Non-cached access

        clock.incrementTimeMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS - 1);
        assertEquals((long) 2, (long) actor.getIndexTally(1).join()); // Still a cached access

        clock.incrementTimeMillis(1);
        assertEquals((long) 3, (long) actor.getIndexTally(1).join()); // Non-cached access
    }

    @Test
    public void testMultipleActors()
    {
        // Setup - Create a few actors
        Set<CacheResponse> actors = new HashSet<>();
        IntStream.range(0, 4).forEach(i -> {
            String id = UUID.randomUUID().toString();
            CacheResponse cacheResponse = Actor.getReference(CacheResponse.class, id);
            actors.add(cacheResponse);
        });

        // Test -- verify that actors do not cache each other
        Set<Long> responses = new HashSet<>();
        actors.forEach(actor -> {
            long response = actor.getNow("greeting").join();

            assertEquals(responses.size() + 1, CacheResponseActor.accessCount);
            assertFalse(responses.contains(response));

            responses.add(response);
        });
    }

    @Test
    public void testMultipleInputs()
    {
        CacheResponse actor = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());

        // Verify that different inputs are separately cached
        Set<Long> responses = new HashSet<>();
        IntStream.range(0, 10).forEach(i -> {
            long response = actor.getNow(String.valueOf(i)).join();

            assertEquals(i + 1, CacheResponseActor.accessCount);
            assertFalse(responses.contains(response));

            responses.add(response);
        });
    }

    @Test(timeout = 10_000L)
    public void testCacheFlush()
    {
        DefaultResponseCachingExtension.setClock(clock);

        CacheResponse actor = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());

        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // New access
        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // Cached access

        actor.flush().join();
        assertEquals((long) 2, (long) actor.getIndexTally(1).join()); // Not a cached access
    }

    @Test(timeout = 10_000L)
    public void testCacheFlushWithMultipleActors()
    {
        CacheResponse actor1 = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());
        CacheResponse actor2 = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());
        final String greetingValue = "hi";

        // Setup: get two actors, caching a different value for the same input
        Long a1Time1 = actor1.getNow(greetingValue).join();
        Long a2Time1 = actor2.getNow(greetingValue).join();

        assertNotEquals(a1Time1, a2Time1);

        // Test: flush one actor, verify that flushed actor is not cached, and non-flushed actor is cached
        actor1.flush().join();
        Long a1Time2 = actor1.getNow(greetingValue).join();
        Long a2Time1Cached = actor2.getNow(greetingValue).join();

        assertNotEquals(a1Time1, a1Time2); // flushed actor is not cached
        assertEquals(a2Time1, a2Time1Cached); // nonflushed actor is still cached
    }

    @Test(timeout = 10_000L)
    public void testCacheFlushWithMultipleInputs()
    {
        CacheResponse actor1 = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());
        final String greetingValue1 = "hi1";
        final String greetingValue2 = "hi2";

        // Setup: cache values for two different inputs
        Long input1Time1 = actor1.getNow(greetingValue1).join();
        Long input2Time1 = actor1.getNow(greetingValue2).join();

        assertNotEquals(input1Time1, input2Time1);

        // Test: flush the actor, verify that neither input was cached
        actor1.flush().join();
        Long input1Time2 = actor1.getNow(greetingValue1).join();
        Long input2Time2 = actor1.getNow(greetingValue2).join();

        assertNotEquals(input1Time1, input1Time2);
        assertNotEquals(input2Time1, input2Time2);
    }

    @Before
    public void initializeStage()
    {
        try
        {
            stage = createStage();
        } catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Before
    public void initializeCacheResponseActor()
    {
        CacheResponseActor.accessCount = 0;
    }

    @Before
    public void initializeCacheManager()
    {
        DefaultResponseCachingExtension.setCacheExecutor(Runnable::run);
        DefaultResponseCachingExtension.setClock(null);
    }

    @After
    public void stopStage()
    {
        if (stage.getState() == NodeCapabilities.NodeState.RUNNING)
        {
            stage.stop().join();
        }
    }
}
