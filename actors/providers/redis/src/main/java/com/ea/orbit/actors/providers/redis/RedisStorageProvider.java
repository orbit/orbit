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

package com.ea.orbit.actors.providers.redis;

import com.ea.orbit.actors.providers.IStorageProvider;
import com.ea.orbit.actors.providers.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

public class RedisStorageProvider implements IStorageProvider {

	private Jedis redis;
	private ObjectMapper mapper;

	private String host = "localhost";
	private int port = 6379;
	private String databaseName;

	@Override
	public Task<Void> start() {
		mapper = new ObjectMapper();
		mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		redis = new Jedis(host, port);
		return Task.done();
	}

	private String asKey(final ActorReference reference) {
		String clazzName = ActorReference.getInterfaceClass(reference).getName();
		String id = String.valueOf(ActorReference.getId(reference));
		return databaseName + "_" + clazzName + "_" + id;
	}

	@Override
	public Task<Void> clearState(final ActorReference reference, final Object state) {
		redis.del(asKey(reference));
		return Task.done();
	}

	@Override
	public Task<Void> stop() {
		redis.close();
		return Task.done();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Task<Boolean> readState(final ActorReference reference, final Object state) {
		String data = redis.get(asKey(reference));
		if (data != null) {
			try {
				mapper.readerForUpdating(state).readValue(data);
			} catch (Exception e) {
				throw new UncheckedException(e);
			}
		}
		return Task.fromValue(true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Task<Void> writeState(final ActorReference reference, final Object state) {
		String data;
		try {
			data = mapper.writeValueAsString(state);
		} catch (JsonProcessingException e) {
			throw new UncheckedException(e);
		}
		redis.set(asKey(reference), data);
		return Task.done();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

}
