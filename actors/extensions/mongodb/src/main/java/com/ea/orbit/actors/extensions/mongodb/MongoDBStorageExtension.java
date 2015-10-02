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

package com.ea.orbit.actors.extensions.mongodb;

import com.ea.orbit.actors.extensions.StorageExtension;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.mongojack.JacksonDBCollection;
import org.mongojack.internal.MongoJackModule;
import org.mongojack.internal.object.BsonObjectTraversingParser;

import java.util.ArrayList;

public class MongoDBStorageExtension implements StorageExtension
{

    private MongoClient mongoClient;
    private ObjectMapper mapper;

    private String user;
    private String database;
    private String host = "localhost";
    private int port = 27017;
    private String password;
    private String name = "default";

    public void setHost(final String host)
    {
        this.host = host;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public void setUser(final String user)
    {
        this.user = user;
    }

    public MongoDBStorageExtension()
    {

    }

    @Override
    public Task<Void> start()
    {
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        MongoJackModule.configure(mapper);

        final ArrayList<MongoCredential> credentials = new ArrayList<>();
        if (user != null && password != null)
        {
            credentials.add(MongoCredential.createPlainCredential(user, database, password.toCharArray()));
        }
        mongoClient = new MongoClient(new ServerAddress(host, port), credentials);

        return Task.done();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Task<Void> clearState(final ActorReference<?> reference, final Object state)
    {
        DB db = mongoClient.getDB(database);
        final DBCollection col = db.getCollection(ActorReference.getInterfaceClass(reference).getSimpleName());
        col.remove(new BasicDBObject("_id", String.valueOf(ActorReference.getId(reference))));
        return Task.done();
    }

    @Override
    public Task<Void> stop()
    {
        mongoClient.close();
        return Task.done();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Boolean> readState(final ActorReference<?> reference, final Object state)
    {
        DB db = mongoClient.getDB(database);
        final DBCollection col = db.getCollection(ActorReference.getInterfaceClass(reference).getSimpleName());
        JacksonDBCollection<Object, String> coll = JacksonDBCollection.wrap(
                col, (Class<Object>) state.getClass(), String.class, mapper);

        DBObject obj = col.findOne(String.valueOf(ActorReference.getId(reference)));
        if (obj != null)
        {
            try
            {
                obj.removeField("_id");
                mapper.readerForUpdating(state).readValue(new BsonObjectTraversingParser(
                        coll, obj, mapper));
                return Task.fromValue(true);
            }
            catch (Exception e)
            {
                throw new UncheckedException("Error reading state of: " + reference, e);
            }
        }
        return Task.fromValue(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Void> writeState(final ActorReference<?> reference, final Object state)
    {
        DB db = mongoClient.getDB(database);
        final DBCollection col = db.getCollection(ActorReference.getInterfaceClass(reference).getSimpleName());
        JacksonDBCollection<Object, String> coll = JacksonDBCollection.wrap(
                col, (Class<Object>) state.getClass(), String.class, mapper);
        DBObject obj = coll.convertToDbObject(state);
        obj.put("_id", String.valueOf(ActorReference.getId(reference)));
        col.save(obj);
        return Task.done();
    }

    public void setDatabase(final String database)
    {
        this.database = database;
    }

    public void setName(final String name)
    {
        this.name = name;
    }
}
