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

package com.ea.orbit.actors.extensions.memcached;

import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.test.StorageTest;
import com.ea.orbit.actors.test.StorageTestState;
import com.ea.orbit.actors.test.StorageBaseTest;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whalin.MemCached.MemCachedClient;

public class MemcachedPersistenceTest extends StorageBaseTest
{

    private ObjectMapper mapper;

    private MemCachedClient memcachedClient;

    @Override
    public Class<? extends StorageTest> getActorInterfaceClass()
    {
        return Hello.class;
    }

    @Override
    public ActorExtension getStorageExtension()
    {
        MemcachedStorageExtension storageExtension = new MemcachedStorageExtension();
        storageExtension.setUseShortKeys(false);
        return storageExtension;
    }

    @Override
    public void initStorage()
    {
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        memcachedClient = MemcachedClientFactory.getClient();
        closeStorage();
    }

    @Override
    public void closeStorage()
    {
        for (int i = 0; i < heavyTestSize(); i++)
        {
            memcachedClient.delete(asKey(getActorInterfaceClass(), String.valueOf(i)));
        }
    }

    public long count(Class<? extends StorageTest> actorInterface)
    {
        int count = 0;
        for (int i = 0; i < heavyTestSize(); i++)
        {
            if (memcachedClient.get(asKey(actorInterface, String.valueOf(i))) != null)
            {
                count++;
            }
        }
        return count;
    }

    private String asKey(final Class<? extends StorageTest> actor, String identity)
    {
        return actor.getName() + MemcachedStorageExtension.KEY_SEPARATOR + identity;
    }

    @Override
    public StorageTestState readState(final String identity)
    {
        Object data = memcachedClient.get(asKey(Hello.class, identity));
        if (data != null)
        {
            try
            {
                return mapper.readValue(data.toString(), HelloState.class);
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
        return count(Hello.class);
    }

    @Override
    public int heavyTestSize()
    {
        return 1000;
    }


}
