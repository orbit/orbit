/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cloud.orbit.actors.extensions.StorageExtension;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryJSONStorageExtension implements StorageExtension
{
    private ConcurrentMap<Object, Object> database;
    private ObjectMapper mapper = new ObjectMapper();
    private String name;

    @Override
    public String getName()
    {
        return name;
    }

    public InMemoryJSONStorageExtension()
    {
        this(new ConcurrentHashMap<>());
    }

    public InMemoryJSONStorageExtension(final ConcurrentMap<Object, Object> database)
    {
        this("default", database);
    }

    public InMemoryJSONStorageExtension(String name, final ConcurrentMap<Object, Object> database)
    {
        this.name = name;
        this.database = database;
        mapper.registerModule(new ActorReferenceModule(DefaultDescriptorFactory.get()));
        mapper.enableDefaultTyping();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    @Override
    public Task<Void> clearState(final RemoteReference<?> reference, final Object state)
    {
        database.remove(reference);
        return Task.done();
    }

    @Override
    public Task<Void> stop()
    {
        return Task.done();
    }

    @Override
    public Task<Boolean> readState(final RemoteReference<?> reference, final Object state)
    {
        String databaseObject = (String) database.get(reference);
        if (databaseObject != null)
        {
            synchronized (this)
            {
                try
                {
                    mapper.readerForUpdating(state).readValue(databaseObject);
                }
                catch (IOException e)
                {
                    throw new UncheckedException(e);
                }
            }
        }
        return Task.fromValue(databaseObject != null);
    }

    @Override
    public Task<Void> writeState(final RemoteReference<?> reference, final Object state)
    {
        try
        {
            database.put(reference, mapper.writeValueAsString(state));
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedException(e);
        }
        return Task.done();
    }
}
