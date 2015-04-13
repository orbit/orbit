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

package com.ea.orbit.actors.providers.jpa.test;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.providers.jpa.JpaStorageProvider;
import com.ea.orbit.actors.test.FakeClusterPeer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class JpaPersistenceTest
{

    private String clusterName = "cluster." + Math.random();
    private ObjectMapper objectMapper;
    private EntityManagerFactory emf;

    @Test
    public void checkWritesTest() throws Exception
    {
        OrbitStage stage = createStage();
        assertEquals(0, count(IHelloActor.class));
        IHelloActor helloActor = IActor.getReference(IHelloActor.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count(IHelloActor.class));
    }

    @Test
    public void checkReadTest() throws Exception
    {
        OrbitStage stage = createStage();
        IHelloActor helloActor = IActor.getReference(IHelloActor.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(readHelloState("300").lastName, "Meep Meep");
    }

    @Test
    public void checkClearTest() throws Exception
    {
        OrbitStage stage = createStage();
        assertEquals(0, count(IHelloActor.class));
        IHelloActor helloActor1 = IActor.getReference(IHelloActor.class, "300");
        helloActor1.sayHello("Meep Meep").join();
        IHelloActor helloActor2 = IActor.getReference(IHelloActor.class, "301");
        helloActor2.sayHello("Meep Meep").join();
        assertEquals(2, count(IHelloActor.class));
        helloActor1.clear().join();
        assertEquals(1, count(IHelloActor.class));
        helloActor2.clear().join();
        assertEquals(0, count(IHelloActor.class));
    }

    @Test
    public void checkUpdateTest() throws Exception
    {
        OrbitStage stage = createStage();
        assertEquals(0, count(IHelloActor.class));
        IHelloActor helloActor = IActor.getReference(IHelloActor.class, "300");
        helloActor.sayHello("Meep Meep").join();
        assertEquals(1, count(IHelloActor.class));
        helloActor.sayHello("Peem Peem").join();
        assertEquals(readHelloState("300").lastName, "Peem Peem");
    }

    public OrbitStage createStage() throws Exception
    {
        OrbitStage stage = new OrbitStage();
        final JpaStorageProvider storageProvider = new JpaStorageProvider();
        storageProvider.setPersistenceUnit("jpa-storage-test");
        stage.addProvider(storageProvider);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.bind();
        return stage;
    }

    @Before
    public void setup() throws Exception
    {
        this.objectMapper = new ObjectMapper();
    }

    @After
    public void cleanup() throws Exception
    {
        emf = Persistence.createEntityManagerFactory("jpa-storage-test");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("delete from " + HelloState.class.getSimpleName());
        em.getTransaction().begin();
        query.executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    private long count(Class<?> actorInterface) throws Exception
    {
        emf = Persistence.createEntityManagerFactory("jpa-storage-test");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("select count(s) from " + HelloState.class.getSimpleName() + " s");
        long count = (long) query.getSingleResult();
        em.close();
        return count;
    }

    private HelloState readHelloState(String identity) throws Exception
    {
        emf = Persistence.createEntityManagerFactory("jpa-storage-test");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("select s from " + HelloState.class.getSimpleName() + " s where s.stateId=:stateId");
        query.setParameter("stateId", identity);
        HelloState state = (HelloState) query.getSingleResult();
        em.close();
        return state;
    }

}
