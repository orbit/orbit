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

import cloud.orbit.actors.Stage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class ConsistentHashTest extends ActorBaseTest
{
    @Test
    public void lonelyServerTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        assertTrue(stage1.getHosting().isConsistentHashOwner("test1"));
        assertTrue(stage1.getHosting().isConsistentHashOwner("test2"));
        assertTrue(stage1.getHosting().isConsistentHashOwner("test3"));
    }

    @Test
    public void lonelyServerABunchOfKeys() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        for (int i = 0; i < 1000; i++)
        {
            String key = "" + Math.random();
            assertTrue(stage1.getHosting().isConsistentHashOwner(key));
        }
    }

    @Test
    public void multipleServerTest() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).count());
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test2")).count());
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test3")).count());
        stages.add(createStage());
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).count());
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test2")).count());
        assertEquals(1, stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test3")).count());
    }

    @Test
    public void migrationTest() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }
        final Stage stage1 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).findFirst().get();

        stages.remove(stage1);
        stage1.stop().join();

        final Stage stage3 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).findFirst().get();
        assertNotSame(stage1, stage3);
    }

    @Test
    public void migrationWithConsistencyTest() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }
        final Stage stage1 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).findFirst().get();

        String secondKey[] = new String[]{"" + Math.random()};
        Stage stage2 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner(secondKey[0])).findFirst().get();
        while (stage1 == stage2)
        {
            secondKey[0] = "" + Math.random();
            stage2 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner(secondKey[0])).findFirst().get();
        }

        stages.remove(stage1);
        stage1.stop().join();

        final Stage stage3 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner("test1")).findFirst().get();
        assertNotSame(stage1, stage3);

        final Stage stage4 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner(secondKey[0])).findFirst().get();
        assertSame(stage2, stage4);
    }

    @Test
    public void migrationInDeletionWithMassConsistencyTest() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }

        // mass create keys
        Map<String, Stage> stableKeys = new HashMap<>();
        while (stableKeys.size() < 100)
        {
            String secondKey = "" + Math.random();
            Stage stage2 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner(secondKey)).findFirst().get();
            stableKeys.put(secondKey, stage2);
        }
        Stage stage1 = stages.get(0);
        stages.remove(stage1);
        stage1.stop().join();

        // verify all stables keys haven't moved
        stableKeys.forEach((k, s) -> {
            Stage newStage = stages.stream().filter(g -> g.getHosting().isConsistentHashOwner(k)).findFirst().get();
            if (s == stage1)
            {
                assertNotSame(stage1, newStage);
            }
            else
            {
                assertSame(s, newStage);
            }
        });
    }

    @Test
    public void migrationWithMassConsistencyTest() throws ExecutionException, InterruptedException
    {
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            stages.add(createStage());
        }

        // mass create keys
        Map<String, Stage> stableKeys = new HashMap<>();
        while (stableKeys.size() < 1000)
        {
            String secondKey = "" + Math.random();
            Stage stage2 = stages.stream().filter(s -> s.getHosting().isConsistentHashOwner(secondKey)).findFirst().get();
            stableKeys.put(secondKey, stage2);
        }
        Stage stage1 = createStage();
        stages.add(stage1);
        AtomicInteger changedCount = new AtomicInteger(0);
        AtomicInteger sameCount = new AtomicInteger(0);

        // verify all stables keys haven't moved
        stableKeys.forEach((k, s) -> {
            Stage newStage = stages.stream().filter(g -> g.getHosting().isConsistentHashOwner(k)).findFirst().get();
            if (stage1 != newStage)
            {
                assertSame(s, newStage);
                sameCount.incrementAndGet();
            }
            else
            {
                changedCount.incrementAndGet();
            }
        });
        //System.out.println("Stable Keys: " + sameCount);
        //System.out.println("Changed Keys: " + changedCount);
    }
}
