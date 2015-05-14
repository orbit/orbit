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

package com.ea.orbit.actors.extensions.postgresql.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.postgresql.PostgreSQLStorageExtension;
import com.ea.orbit.actors.test.FakeClusterPeer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class PostgreSQLPersistenceTest
{

    private String clusterName = "cluster." + Math.random();
    private Connection conn;
    private ObjectMapper objectMapper;

    @Test
    public void checkWritesTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count(Hello.class));
        Hello helloActor = Actor.getReference(Hello.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count(Hello.class));
    }

    @Test
    public void checkReadTest() throws Exception
    {
        Stage stage = createStage();
        Hello helloActor = Actor.getReference(Hello.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(readHelloState("300").lastName, "Meep Meep");
    }

    @Test
    public void checkClearTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count(Hello.class));
        Hello helloActor = Actor.getReference(Hello.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count(Hello.class));
        helloActor.clear().join();
        assertEquals(0, count(Hello.class));
    }

    @Test
    public void checkUpdateTest() throws Exception
    {
        Stage stage = createStage();
        assertEquals(0, count(Hello.class));
        Hello helloActor = Actor.getReference(Hello.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count(Hello.class));
        helloActor.sayHello("Peem Peem").join();
        assertEquals(readHelloState("300").lastName, "Peem Peem");
    }

    public Stage createStage() throws Exception
    {
        Stage stage = new Stage();
        final PostgreSQLStorageExtension storageExtension = new PostgreSQLStorageExtension();
        storageExtension.setPort(5432);
        storageExtension.setDatabase("orbit");
        storageExtension.setUsername("postgres");
        storageExtension.setPassword(null);
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
        Class.forName("org.postgresql.Driver");
        this.conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/orbit", "postgres", null);
        this.objectMapper = new ObjectMapper();
    }

    @After
    public void cleanup() throws Exception
    {
        Statement dropStmt = this.conn.createStatement();
        dropStmt.execute("DROP TABLE actor_states");
        dropStmt.close();
        this.conn.close();
    }

    private int count(Class<?> actorInterface) throws Exception
    {
        Statement stmt = this.conn.createStatement();
        String name = actorInterface.getSimpleName();
        ResultSet results = stmt.executeQuery("SELECT COUNT(*) AS \"cnt\" FROM actor_states WHERE actor = '" + name + "'");
        results.next();
        int count = results.getInt("cnt");
        stmt.close();
        return count;
    }

    private HelloActor.State readHelloState(String identity) throws Exception
    {
        Statement stmt = this.conn.createStatement();
        ResultSet results = stmt.executeQuery(
                "SELECT state AS \"state\" FROM actor_states WHERE actor = '" + Hello.class.getSimpleName()
                        + "' AND identity = '" + identity + "'");
        results.next();
        HelloActor.State state = objectMapper.readValue(results.getString("state"), HelloActor.State.class);
        stmt.close();
        return state;
    }

}
