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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.ExecutionCacheManager;
import com.ea.orbit.actors.runtime.NodeCapabilities;
import com.ea.orbit.actors.test.actors.CacheResponse;
import com.ea.orbit.actors.test.actors.CacheResponseActor;
import com.ea.orbit.exception.UncheckedException;
import com.google.common.base.Ticker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CacheResponseTest extends ActorBaseTest
{
    /**
     * A Ticker that does not progress time unless called with advanceMillis
     */
    private class CacheResponseTestTicker extends Ticker
    {
        private long elapsedMillis;

        @Override
        public long read()
        {
            return TimeUnit.MILLISECONDS.toNanos(elapsedMillis);
        }

        public void advanceMillis(long millis)
        {
            elapsedMillis += millis;
        }
    }

    private Stage stage;

    @Test
    public void testCacheDuration()
    {
        CacheResponseTestTicker ticker = new CacheResponseTestTicker();
        ExecutionCacheManager.setDefaultCacheTicker(ticker);

        CacheResponse actor = Actor.getReference(CacheResponse.class, UUID.randomUUID().toString());

        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // New access
        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // Cached access

        ticker.advanceMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS / 2);
        assertEquals((long) 1, (long) actor.getIndexTally(1).join()); // Still a cached access

        // Advance a Duration cycle
        ticker.advanceMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS);
        assertEquals((long) 2, (long) actor.getIndexTally(1).join()); // Non-cached access

        ticker.advanceMillis(CacheResponseActor.INDEX_TALLY_DURATION_MILLIS - 1);
        assertEquals((long) 2, (long) actor.getIndexTally(1).join()); // Still a cached access

        ticker.advanceMillis(1);
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
            sleep(10);
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
            sleep(10);
            long response = actor.getNow(String.valueOf(i)).join();

            assertEquals(i + 1, CacheResponseActor.accessCount);
            assertFalse(responses.contains(response));

            responses.add(response);
        });
    }

    public static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
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
        ExecutionCacheManager.setDefaultCacheTicker(null);
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
