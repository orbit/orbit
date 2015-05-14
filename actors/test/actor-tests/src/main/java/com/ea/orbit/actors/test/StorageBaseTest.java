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
import com.ea.orbit.actors.extensions.ActorExtension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class StorageBaseTest
{

    private String clusterName = "cluster." + Math.random();

    @Test
    public void checkWritesTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count());
        StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count());
    }

    @Test
    public void heavyTest() throws Exception
    {
        // not a load test "per se" but rather a test with more than a few calls.
        Stage stage = createStage();
        assertEquals(0, count());
        for(int t=0;t< heavyTestSize();t++){
            String id =""+t;
            StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), id);
            helloActor.sayHello("Meep Meep"+t).join();
        }
        assertEquals(heavyTestSize(), count());
        for(int t=0;t< heavyTestSize();t++){
            String id =""+t;
            StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), id);
            helloActor.sayHello("Meep Meep"+t).join();
            assertEquals(readState(id).lastName(), "Meep Meep"+t);
        }
        for(int t=0;t< heavyTestSize();t++){
            String id =""+t;
            StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), id);
            helloActor.clear().join();
        }
        assertEquals(0, count());
    }

    @Test
    public void checkReadTest() throws Exception
    {
        Stage stage = createStage();
        StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(readState("300").lastName(), "Meep Meep");
    }

    @Test
    public void checkClearTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count());
        StorageTest helloActor1 = Actor.getReference(getActorInterfaceClass(), "300");
        helloActor1.sayHello("Meep Meep").join();
        StorageTest helloActor2 = Actor.getReference(getActorInterfaceClass(), "301");
        helloActor2.sayHello("Meep Meep").join();
        assertEquals(2, count());
        helloActor1.clear().join();
        assertEquals(1, count());
        helloActor2.clear().join();
        assertEquals(0, count());
    }

    @Test
    public void checkUpdateTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count());
        StorageTest helloActor = Actor.getReference(getActorInterfaceClass(), "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count());
        helloActor.sayHello("Peem Peem").join();
        assertEquals(readState("300").lastName(), "Peem Peem");
    }

    public Stage createStage() throws Exception
    {
        Stage stage = new Stage();
        stage.addExtension(getStorageProvider());
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.bind();
        return stage;
    }

    @Before
    public void setup() throws Exception
    {
        initStorage();
    }

    @After
    public void cleanup() throws Exception
    {
        closeStorage();
    }

    public abstract Class<? extends StorageTest> getActorInterfaceClass();

    public abstract ActorExtension getStorageProvider();

    public abstract void initStorage();

    public abstract void closeStorage();

    public abstract long count();

    public abstract int heavyTestSize();

    public abstract StorageTestState readState(String identity);


}
