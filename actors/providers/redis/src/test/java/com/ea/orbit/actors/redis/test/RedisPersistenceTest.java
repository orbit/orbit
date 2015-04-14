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

package com.ea.orbit.actors.redis.test;

import com.ea.orbit.actors.providers.IOrbitProvider;
import com.ea.orbit.actors.providers.json.ActorReferenceModule;
import com.ea.orbit.actors.providers.redis.RedisStorageProvider;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.test.IStorageTestActor;
import com.ea.orbit.actors.test.IStorageTestState;
import com.ea.orbit.actors.test.StorageBaseTest;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;

public class RedisPersistenceTest extends StorageBaseTest
{

    private Jedis database;
    private String databaseName;
    private ObjectMapper mapper;


    @Override
    public Class<? extends IStorageTestActor> getActorInterfaceClass()
    {
        return IHelloActor.class;
    }

    @Override
    public IOrbitProvider getStorageProvider()
    {
        final RedisStorageProvider storageProvider = new RedisStorageProvider();
        storageProvider.setDatabaseName(databaseName);
        return storageProvider;
    }

    @Override
    public void initStorage()
    {
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        databaseName = "" + (int) (Math.random() * Integer.MAX_VALUE);
        database = new Jedis("localhost", 6379);
    }

    @Override
    public void closeStorage()
    {
        database.flushDB();
    }

    public long count(Class<?> actorInterface)
    {
        String classname = actorInterface.getSimpleName();
        return database.keys(databaseName + "_" + classname + "*").size();
    }

    @Override
    public IStorageTestState readState(final String identity)
    {
        String data = database.get(databaseName + "_" + IHelloActor.class.getSimpleName() + "_" + identity);
        if (data != null)
        {
            try
            {
                return mapper.readValue(data, HelloState.class);
            }
            catch (Exception e)
            {
                throw new UncheckedException(e);
            }
        }
        return null;
    }


    public long count()
    {
        return database.keys(databaseName + "_" + IHelloActor.class.getSimpleName() + "*").size();
    }

    @Override
    public int loadTestSize()
    {
        return 10000;
    }


}
