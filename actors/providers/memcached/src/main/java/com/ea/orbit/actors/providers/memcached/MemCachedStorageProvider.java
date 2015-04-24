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
import com.ea.orbit.actors.providers.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whalin.MemCached.MemCachedClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link MemCachedStorageProvider} provides Memcached support for storing actor states.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class MemCachedStorageProvider implements IStorageProvider {

	private MemCachedClient memCachedClient;
	private ObjectMapper mapper;

	private boolean useShortKeys = false;
	private boolean serializeStateAsJson = false;

	@Override
	public Task<Void> start() {
		mapper = new ObjectMapper();
		mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		if (memCachedClient == null) {
			memCachedClient = MemCachedClientFactory.getClient();
		}
		return Task.done();
	}

	private String asKey(final ActorReference reference) {
		String clazzName = useShortKeys ? ActorReference.getInterfaceClass(reference).getSimpleName() : ActorReference.getInterfaceClass(reference).getName();
		return clazzName + "|" + String.valueOf(ActorReference.getId(reference));
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
	public Task<Void> readState(final ActorReference<?> reference, final AtomicReference<Object> stateReference)
	{
		Object data = memCachedClient.get(asKey(reference));
		if (data != null) {
			if (serializeStateAsJson) {
				try {
					mapper.readerForUpdating(stateReference.get()).readValue(data.toString());
				} catch (Exception e) {
					throw new UncheckedException("Error parsing memcached response: " + data, e);
				}
			} else {
				stateReference.set(data);
			}
		}
		return Task.done();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Task<Void> writeState(final ActorReference reference, final Object state) {
		if (serializeStateAsJson) {
			String data;
			try {
				data = mapper.writeValueAsString(state);
			} catch (JsonProcessingException e) {
				throw new UncheckedException(e);
			}
			memCachedClient.set(asKey(reference), data);
		} else {
			memCachedClient.set(asKey(reference), state);
		}
		return Task.done();
	}

	public void setUseShortKeys(final boolean useShortKeys)
	{
		this.useShortKeys = useShortKeys;
	}

	public void setSerializeStateAsJson(final boolean serializeStateAsJson)
	{
		this.serializeStateAsJson = serializeStateAsJson;
	}

	public void setMemCachedClient(final MemCachedClient memCachedClient)
	{
		this.memCachedClient = memCachedClient;
	}

}
