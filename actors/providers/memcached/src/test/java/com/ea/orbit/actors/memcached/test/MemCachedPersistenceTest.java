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

import com.ea.orbit.actors.providers.IOrbitProvider;
import com.ea.orbit.actors.providers.json.ActorReferenceModule;
import com.ea.orbit.actors.providers.memcached.MemCachedClientFactory;
import com.ea.orbit.actors.providers.memcached.MemCachedStorageProvider;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.test.IStorageTestActor;
import com.ea.orbit.actors.test.IStorageTestState;
import com.ea.orbit.actors.test.StorageBaseTest;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whalin.MemCached.MemCachedClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemCachedPersistenceTest extends StorageBaseTest
{

    private MemCachedClient memCachedClient;
    private ObjectMapper mapper;

    @Override
    public Class<? extends IStorageTestActor> getActorInterfaceClass()
    {
        return IHelloActor.class;
    }

    @Override
    public IOrbitProvider getStorageProvider()
    {
        MemCachedStorageProvider storageProvider = new MemCachedStorageProvider();
        storageProvider.setSerializeStateAsJson(true);
        storageProvider.setUseShortKeys(false);
        return storageProvider;
    }

    @Override
    public void initStorage()
    {
        memCachedClient = MemCachedClientFactory.getClient();

        closeStorage();

        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    @Override
    public void closeStorage()
    {
        memCachedClient.flushAll();
    }

    private static final Pattern ITEMS_PATTERN = Pattern.compile("items:(\\d+):number");

    // couldn't find any other ways to count items in the Memcached instance..
    public long count(Class<?> actorInterface)
    {
        int count = 0;
        Map<String, Map<String, String>> statsItems = memCachedClient.statsItems();

        for (Map<String, String> items : statsItems.values())
        {
            for (String item : items.keySet())
            {
                Matcher matcher =  ITEMS_PATTERN.matcher(item);
                if (matcher.matches())
                {
                    int slabNumber = Integer.valueOf(matcher.group(1));
                    Map<String, Map<String, String>> cacheDump = memCachedClient.statsCacheDump(slabNumber, 10000);
                    for (Map<String, String> cacheItem : cacheDump.values())
                    {
                        for (String key : cacheItem.keySet())
                        {
                            if (key.startsWith(actorInterface.getName())) {
                                count++;
                            }
                        }
                    }

                }
            }
        }
        return count;
    }

    @Override
    public IStorageTestState readState(final String identity)
    {
        Object data = memCachedClient.get(IHelloActor.class.getName() + "|" + identity);
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
        return count(IHelloActor.class);
    }

    @Override
    public int heavyTestSize()
    {
        return 100;
    }


}
