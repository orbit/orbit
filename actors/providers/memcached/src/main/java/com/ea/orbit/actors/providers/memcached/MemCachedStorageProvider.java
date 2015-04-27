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

package com.ea.orbit.actors.providers.memcached;

import com.ea.orbit.actors.providers.IStorageProvider;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.concurrent.Task;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;

import com.whalin.MemCached.MemCachedClient;

import static com.ea.orbit.actors.providers.memcached.MemCachedStorageHelper.*;

/**
 * {@link MemCachedStorageProvider} provides Memcached support for storing actor states.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class MemCachedStorageProvider implements IStorageProvider
{

    private Mapper mapper;
    private MemCachedClient memCachedClient;
    private MemCachedStorageHelper memCachedStorageHelper;

    private boolean useShortKeys = false;

    @Override
    public Task<Void> start()
    {
        mapper = new DozerBeanMapper();
        if (memCachedClient == null)
        {
            memCachedClient = MemCachedClientFactory.getClient();
        }
        memCachedStorageHelper = new MemCachedStorageHelper(memCachedClient);
        return Task.done();
    }

    @Override
    public Task<Void> clearState(final ActorReference reference, final Object state)
    {
        memCachedClient.delete(asKey(reference));
        return Task.done();
    }

    @Override
    public Task<Void> stop()
    {
        return Task.done();
    }

    @Override
    public Task<Boolean> readState(final ActorReference<?> reference, final Object state)
    {
        Object value = memCachedStorageHelper.get(asKey(reference));
        if (value != null)
        {
            mapper.map(value, state);
            return Task.fromValue(true);
        }
        return Task.fromValue(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Void> writeState(final ActorReference reference, final Object state)
    {
        memCachedStorageHelper.set(asKey(reference), state);
        return Task.done();
    }

    private String asKey(final ActorReference reference)
    {
        String clazzName = useShortKeys ? ActorReference.getInterfaceClass(reference).getSimpleName() : ActorReference.getInterfaceClass(reference).getName();
        return clazzName + KEY_SEPARATOR + String.valueOf(ActorReference.getId(reference));
    }

    public void setUseShortKeys(final boolean useShortKeys)
    {
        this.useShortKeys = useShortKeys;
    }

    public void setMemCachedClient(final MemCachedClient memCachedClient)
    {
        this.memCachedClient = memCachedClient;
    }

}
