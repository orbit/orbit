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
import com.ea.orbit.actors.test.actors.Storage1;
import com.ea.orbit.actors.test.actors.Storage2;

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
        Stage stage1 = createStage();
        assertEquals(0, fakeDatabase1.values().size());
        assertEquals(0, fakeDatabase2.values().size());

        Storage1 storage1A = Actor.getReference(Storage1.class, "301");
        Storage1 storage1B = Actor.getReference(Storage1.class, "302");
        Storage1 storage1C = Actor.getReference(Storage1.class, "303");
        Storage1 storage1D = Actor.getReference(Storage1.class, "304");
        Storage2 storage2A = Actor.getReference(Storage2.class, "400");
        Storage2 storage2B = Actor.getReference(Storage2.class, "401");

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

        Stage stage2 = createStage();

        Storage1 storage1AA = Actor.getReference(Storage1.class, "301");
        Storage1 storage1BB = Actor.getReference(Storage1.class, "302");
        Storage1 storage1CC = Actor.getReference(Storage1.class, "303");
        Storage1 storage1DD = Actor.getReference(Storage1.class, "304");
        Storage2 storage2AA = Actor.getReference(Storage2.class, "400");
        Storage2 storage2BB = Actor.getReference(Storage2.class, "401");

        assertEquals("I", storage1AA.get().join());
        assertEquals("am", storage1BB.get().join());
        assertEquals("testing", storage1CC.get().join());
        assertEquals("something", storage1DD.get().join());
        assertEquals("really", storage2AA.get().join());
        assertEquals("cool", storage2BB.get().join());

    }

    @Override
    public Stage createStage() throws ExecutionException, InterruptedException
    {
        Stage stage = new Stage();
        stage.setMode(Stage.StageMode.HOST);
        stage.setExecutionPool(commonPool);
        stage.setMessagingPool(commonPool);
        stage.addExtension(new FakeStorageExtension("default", fakeDatabase1));
        stage.addExtension(new FakeStorageExtension("fake2", fakeDatabase2));
        stage.setClock(clock);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().join();
        stage.bind();
        return stage;
    }
}
