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

package com.ea.orbit.actors.extensions.jpa.test;

import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.jpa.JpaStorageExtension;
import com.ea.orbit.actors.test.StorageTest;
import com.ea.orbit.actors.test.StorageTestState;
import com.ea.orbit.actors.test.StorageBaseTest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

@SuppressWarnings("unused")
public class JpaPersistenceTest extends StorageBaseTest
{

    private EntityManagerFactory emf;

    @Override
    public Class<? extends StorageTest> getActorInterfaceClass()
    {
        return Hello.class;
    }

    @Override
    public ActorExtension getStorageProvider()
    {
        final JpaStorageExtension storageProvider = new JpaStorageExtension();
        storageProvider.setPersistenceUnit("jpa-storage-test");
        return storageProvider;
    }

    @Override
    public void initStorage()
    {
        emf = Persistence.createEntityManagerFactory("jpa-storage-test");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("delete from " + HelloState.class.getSimpleName());
        em.getTransaction().begin();
        query.executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public void closeStorage()
    {
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("delete from " + HelloState.class.getSimpleName());
        em.getTransaction().begin();
        query.executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    public long count()
    {
        emf = Persistence.createEntityManagerFactory("jpa-storage-test");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("select count(s) from " + HelloState.class.getSimpleName() + " s");
        long count = (long) query.getSingleResult();
        em.close();
        return count;
    }

    @Override
    public int heavyTestSize()
    {
        return 100;
    }

    @Override
    public StorageTestState readState(final String identity)
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
