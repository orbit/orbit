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

package com.ea.orbit.actors.providers.jpa;

import com.ea.orbit.actors.providers.AbstractStorageProvider;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

public class JpaStorageProvider extends AbstractStorageProvider
{

    private String persistenceUnitName = "jp-storage-production";

    private EntityManagerFactory emf;
    private ObjectMapper mapper;


    public void setPersistenceUnit(final String name)
    {
        persistenceUnitName = name;
    }

    @Override
    public synchronized Task<Void> clearState(final ActorReference<?> reference, final Object state)
    {
        try
        {
            String stateId = getIdentity(reference);
            EntityManager em = emf.createEntityManager();
            Query query = em.createQuery("delete from " + state.getClass().getSimpleName() + " s where s.stateId=:stateId");
            query.setParameter("stateId", stateId);
            em.getTransaction().begin();
            query.executeUpdate();
            em.getTransaction().commit();
            em.close();
            return Task.done();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Override
    public synchronized Task<Boolean> readState(final ActorReference<?> reference, final Object state)
    {
        try
        {
            String stateId = getIdentity(reference);
            EntityManager em = emf.createEntityManager();
            Query query = em.createQuery("select s from " + state.getClass().getSimpleName() + " s where s.stateId=:stateId");
            query.setParameter("stateId", stateId);
            Object newState;
            try
            {
                newState = query.getSingleResult();
                mapper.readerForUpdating(state).readValue(mapper.writeValueAsString(newState));
                return Task.fromValue(true);
            }
            catch (NoResultException ignore)
            {
            }
            finally
            {
                em.close();
            }
            return Task.fromValue(false);
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Override
    public synchronized Task<Void> writeState(final ActorReference<?> reference, final Object state)
    {
        String identity = getIdentity(reference);
        JpaState jpaState = (JpaState) state;
        jpaState.stateId = identity;
        try
        {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.merge(state);
            em.getTransaction().commit();
            em.detach(state);
            em.close();
            return Task.done();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @Override
    public Task<Void> start()
    {
        emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        return Task.done();
    }

    @Override
    public Task<Void> stop()
    {
        try
        {
            emf.close();
            return Task.done();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    private String getIdentity(final ActorReference<?> reference)
    {
        return String.valueOf(ActorReference.getId(reference));
    }
}
