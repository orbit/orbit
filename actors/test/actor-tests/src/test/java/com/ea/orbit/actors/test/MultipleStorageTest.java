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
import com.ea.orbit.actors.test.actors.IStorage1Actor;
import com.ea.orbit.actors.test.actors.IStorage2Actor;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class MultipleStorageTest extends ActorBaseTest
{
    protected ConcurrentHashMap<Object, Object> fakeDatabase1 = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Object, Object> fakeDatabase2 = new ConcurrentHashMap<>();

    @Test
    public void checkWritesTest() throws ExecutionException, InterruptedException
    {
        OrbitStage stage1 = createStage();
        assertEquals(0, fakeDatabase1.values().size());
        assertEquals(0, fakeDatabase2.values().size());

        IStorage1Actor storage1A = IActor.getReference(IStorage1Actor.class, "301");
        IStorage1Actor storage1B = IActor.getReference(IStorage1Actor.class, "302");
        IStorage1Actor storage1C = IActor.getReference(IStorage1Actor.class, "303");
        IStorage1Actor storage1D = IActor.getReference(IStorage1Actor.class, "304");
        IStorage2Actor storage2A = IActor.getReference(IStorage2Actor.class, "400");
        IStorage2Actor storage2B = IActor.getReference(IStorage2Actor.class, "401");

        storage1A.put("I").join();
        storage1B.put("am").join();
        storage1C.put("testing").join();
        storage1D.put("something").join();
        assertEquals(4, fakeDatabase1.values().size());
        assertEquals(0, fakeDatabase2.values().size());
        storage2A.put("really").join();
        storage2B.put("cool").join();

        assertEquals(4, fakeDatabase1.values().size());
        assertEquals(2, fakeDatabase2.values().size());

        OrbitStage stage2 = createStage();

        IStorage1Actor storage1AA = IActor.getReference(IStorage1Actor.class, "301");
        IStorage1Actor storage1BB = IActor.getReference(IStorage1Actor.class, "302");
        IStorage1Actor storage1CC = IActor.getReference(IStorage1Actor.class, "303");
        IStorage1Actor storage1DD = IActor.getReference(IStorage1Actor.class, "304");
        IStorage2Actor storage2AA = IActor.getReference(IStorage2Actor.class, "400");
        IStorage2Actor storage2BB = IActor.getReference(IStorage2Actor.class, "401");

        assertEquals("I", storage1AA.get().join());
        assertEquals("am", storage1BB.get().join());
        assertEquals("testing", storage1CC.get().join());
        assertEquals("something", storage1DD.get().join());
        assertEquals("really", storage2AA.get().join());
        assertEquals("cool", storage2BB.get().join());

    }

    @Override
    public OrbitStage createStage() throws ExecutionException, InterruptedException
    {
        OrbitStage stage = new OrbitStage();
        stage.setMode(OrbitStage.StageMode.HOST);
        stage.setExecutionPool(commonPool);
        stage.setMessagingPool(commonPool);
        stage.addProvider(new FakeStorageProvider("fake1", fakeDatabase1));
        stage.addProvider(new FakeStorageProvider("fake2", fakeDatabase2));
        stage.setClock(clock);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().join();
        stage.bind();
        return stage;
    }
}
