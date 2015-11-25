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

package com.ea.orbit.actors.mongodb.test;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.mongodb.MongoDBStorageExtension;
import com.ea.orbit.actors.test.FakeClusterPeer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.DB;
import com.mongodb.MongoClient;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class MongodbPersistenceTest
{
    private String clusterName = "cluster." + Math.random();
    private DB database;

    @Test
    public void checkWritesTest() throws Exception
    {
        Stage stage1 = createStage();
        assertEquals(0, database.getCollection("SomeMatch").count());
        SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
        SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
        someMatch.addPlayer(somePlayer).get();
        assertEquals(1, database.getCollection("SomeMatch").count());
    }

    @Test
    public void checkReads() throws Exception
    {
        assertEquals(0, database.getCollection("SomeMatch").count());
        {
            // adding some state and then tearing down the cluster.
            Stage stage1 = createStage();
            stage1.bind();
            SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
            SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
            someMatch.addPlayer(somePlayer).get();
            stage1.stop().get(30, TimeUnit.SECONDS);
        }
        assertEquals(1, database.getCollection("SomeMatch").count());
        {
            Stage stage2 = createStage();
            stage2.bind();
            SomeMatch someMatch_r2 = Actor.getReference(SomeMatch.class, "300");
            SomePlayer somePlayer_r2 = Actor.getReference(SomePlayer.class, "101");
            assertEquals(1, someMatch_r2.getPlayers().get().size());
            assertEquals(somePlayer_r2, someMatch_r2.getPlayers().get().get(0));
        }
    }

    @Test
    public void checkClearState() throws Exception
    {
        assertEquals(0, database.getCollection("SomeMatch").count());
        // adding some state and then tearing down the cluster.
        Stage stage1 = createStage();
        SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
        SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
        someMatch.addPlayer(somePlayer).get();
        assertEquals(1, database.getCollection("SomeMatch").count());

        someMatch.delete().get();
        assertEquals(0, database.getCollection("SomeMatch").count());
    }


    public Stage createStage() throws Exception
    {

        Stage stage = new Stage();
        final MongoDBStorageExtension storageExtension = new MongoDBStorageExtension();
        storageExtension.setDatabase(database.getName());
        stage.addExtension(storageExtension);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.bind();

        return stage;
    }

    @Before
    public void setup() throws Exception
    {
        MongoClient client = new MongoClient();
        String databaseName = "test_" + MongodbPersistenceTest.class.getSimpleName() + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
        database = client.getDB(databaseName);
    }

    @After
    public void tearDown() throws Exception
    {
        database.dropDatabase();
    }
}
