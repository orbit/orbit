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

package com.ea.orbit.actors.memcached.test;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.providers.IOrbitProvider;
import com.ea.orbit.actors.providers.memcached.MemCachedClientFactory;
import com.ea.orbit.actors.providers.memcached.MemCachedStorageHelper;
import com.ea.orbit.actors.providers.memcached.MemCachedStorageProvider;
import com.ea.orbit.actors.test.IStorageTestActor;
import com.ea.orbit.actors.test.IStorageTestState;
import com.ea.orbit.actors.test.StorageBaseTest;

import org.junit.Test;

import com.whalin.MemCached.MemCachedClient;

import static com.ea.orbit.actors.providers.memcached.MemCachedStorageHelper.KEY_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MemCachedPersistenceTest extends StorageBaseTest
{

    private MemCachedClient memCachedClient;
    private MemCachedStorageHelper memCachedStorageHelper;

    @Test
    public void testObserverSerialization() throws Exception
    {
        createStage();
        IHelloActor helloActor = IActor.getReference(IHelloActor.class, "1");
        helloActor.addObserver(new HelloObserver()).join();
        HelloState helloState = (HelloState) readState("1");
        helloState.observers.cleanup();
    }

    @Override
    public Class<? extends IStorageTestActor> getActorInterfaceClass()
    {
        return IHelloActor.class;
    }

    @Override
    public IOrbitProvider getStorageProvider()
    {
        MemCachedStorageProvider storageProvider = new MemCachedStorageProvider();
        storageProvider.setUseShortKeys(false);
        return storageProvider;
    }

    @Override
    public void initStorage()
    {
        memCachedClient = MemCachedClientFactory.getClient();
        memCachedStorageHelper = new MemCachedStorageHelper(memCachedClient);
        closeStorage();
    }

    @Override
    public void closeStorage()
    {
        for (int i = 0; i < heavyTestSize(); i++)
        {
            memCachedClient.delete(asKey(getActorInterfaceClass(), String.valueOf(i)));
        }
    }

    public long count(Class<? extends IStorageTestActor> actorInterface)
    {
        int count = 0;
        for (int i = 0; i < heavyTestSize(); i++)
        {
            if (memCachedStorageHelper.get(asKey(actorInterface, String.valueOf(i))) != null)
            {
                count++;
            }
        }
        return count;
    }

    private String asKey(final Class<? extends IStorageTestActor> actor, String identity)
    {
        return actor.getName() + KEY_SEPARATOR + identity;
    }

    @Override
    public IStorageTestState readState(final String identity)
    {
        return (IStorageTestState) memCachedStorageHelper.get(asKey(IHelloActor.class, identity));
    }

    public long count()
    {
        return count(IHelloActor.class);
    }

    @Override
    public int heavyTestSize()
    {
        return 1000;
    }


}
