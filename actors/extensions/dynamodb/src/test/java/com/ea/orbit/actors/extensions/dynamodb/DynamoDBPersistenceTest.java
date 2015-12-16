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

package com.ea.orbit.actors.extensions.dynamodb;

import com.ea.orbit.actors.extensions.ActorExtension;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.test.StorageBaseTest;
import com.ea.orbit.actors.test.StorageTest;
import com.ea.orbit.actors.test.StorageTestState;
import com.ea.orbit.exception.UncheckedException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

public class DynamoDBPersistenceTest extends StorageBaseTest
{
    private ObjectMapper mapper;

    private AmazonDynamoDBClient dynamoClient;
    private DynamoDB dynamoDB;

    @Override
    public Class<? extends StorageTest> getActorInterfaceClass()
    {
        return Hello.class;
    }

    @Override
    public ActorExtension getStorageExtension()
    {
        final DynamoDBStorageExtension extension = new DynamoDBStorageExtension();
        extension.setCredentialType(DynamoDBStorageExtension.AmazonCredentialType.BASIC_CREDENTIALS);
        extension.setAccessKey("dummy");
        extension.setSecretKey("dummy");
        return extension;
    }

    @Override
    public void initStorage()
    {
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(DefaultDescriptorFactory.get()));
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        dynamoClient = new AmazonDynamoDBClient(new BasicAWSCredentials("dummy", "dummy"));
        dynamoClient.setEndpoint("http://localhost:8000/");
        dynamoDB = new DynamoDB(dynamoClient);

        closeStorage();
    }

    @Override
    public void closeStorage()
    {
        try
        {
            dynamoClient.describeTable(getActorInterfaceClass().getSimpleName());
            dynamoClient.deleteTable(getActorInterfaceClass().getSimpleName());
        }
        catch(ResourceNotFoundException e)
        {

        }

        dynamoDB.createTable(getActorInterfaceClass().getSimpleName(),
                Collections.singletonList(
                        new KeySchemaElement("_id", KeyType.HASH)),
                Collections.singletonList(
                       new AttributeDefinition("_id", ScalarAttributeType.S)),
                new ProvisionedThroughput(10L, 10L));
    }

    public long count(Class<? extends StorageTest> actorInterface)
    {
        return dynamoClient.describeTable(actorInterface.getSimpleName()).getTable().getItemCount();
    }

    @Override
    public StorageTestState readState(final String identity)
    {
        final Table table = dynamoDB.getTable(getActorInterfaceClass().getSimpleName());
        final Item item = table.getItem("_id", identity);

        if (item != null)
        {
            try
            {
                final StorageTestState testState = new HelloState();
                mapper.readerForUpdating(testState).readValue(item.getJSON("_state"));
                return testState;
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
        return 100;
    }
}
