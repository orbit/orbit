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

import com.ea.orbit.actors.extensions.StorageExtension;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ExceptionUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class DynamoDBStorageExtension implements StorageExtension
{
    public enum AmazonCredentialType
    {
        DEFAULT_PROVIDER_CHAIN,
        BASIC_CREDENTIALS,
        BASIC_SESSION_CREDENTIALS
    }

    private AmazonDynamoDBAsyncClient dynamoClient;
    private DynamoDB dynamoDB;

    private ConcurrentHashMap<String, Table> tableHashMap;

    private ObjectMapper mapper;
    private String name = "default";

    private String endpoint = "http://localhost:8000/";
    private AmazonCredentialType credentialType = AmazonCredentialType.DEFAULT_PROVIDER_CHAIN;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private boolean shouldCreateTables = true;


    public DynamoDBStorageExtension()
    {

    }

    @Override
    public Task<Void> start()
    {
        tableHashMap = new ConcurrentHashMap<>();
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(DefaultDescriptorFactory.get()));
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));


        switch(credentialType)
        {
            case BASIC_CREDENTIALS:
                dynamoClient = new AmazonDynamoDBAsyncClient(new BasicAWSCredentials(accessKey, secretKey));
                break;

            case BASIC_SESSION_CREDENTIALS:
                dynamoClient = new AmazonDynamoDBAsyncClient(new BasicSessionCredentials(accessKey, secretKey, sessionToken));
                break;

            case DEFAULT_PROVIDER_CHAIN:
            default:
                dynamoClient = new AmazonDynamoDBAsyncClient(new DefaultAWSCredentialsProviderChain());
                break;
        }

        dynamoClient.setEndpoint(endpoint);

        dynamoDB = new DynamoDB(dynamoClient);

        return Task.done();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Task<Void> clearState(final RemoteReference<?> reference, final Object state)
    {
        return getOrCreateTable(RemoteReference.getInterfaceClass(reference).getSimpleName())
                .thenAccept(table -> table.deleteItem("_id", String.valueOf(RemoteReference.getId(reference))));
    }

    @Override
    public Task<Void> stop()
    {
        return Task.done();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Boolean> readState(final RemoteReference<?> reference, final Object state)
    {

        return getOrCreateTable(RemoteReference.getInterfaceClass(reference).getSimpleName())
                .thenApply(table -> table.getItem("_id", String.valueOf(RemoteReference.getId(reference))))
                .thenApply(item ->
                {
                    if (item != null)
                    {
                        try
                        {
                            mapper.readerForUpdating(state).readValue(item.getJSON("_state"));
                            return true;
                        }
                        catch (IOException e)
                        {
                            throw new UncheckedException(e);
                        }
                    }
                    else
                    {
                        return false;
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Void> writeState(final RemoteReference<?> reference, final Object state)
    {
        try
        {
            final String serializedState = mapper.writeValueAsString(state);

            return getOrCreateTable(RemoteReference.getInterfaceClass(reference).getSimpleName())
                    .thenAccept(table -> table.putItem(new Item().withPrimaryKey("_id", String.valueOf(RemoteReference.getId(reference))).withJSON("_state", serializedState)));
        }
        catch (JsonProcessingException e)
        {
            throw new UncheckedException(e);
        }
    }

    private Task<Table> getOrCreateTable(final String tableName)
    {
        final Table table = tableHashMap.get(tableName);
        if (table != null)
        {
            return Task.fromValue(table);
        }
        else
        {
            return Task.fromFuture(dynamoClient.describeTableAsync(tableName))
                    .thenApply(DescribeTableResult::getTable)
                    .thenApply(descriptor -> dynamoDB.getTable(descriptor.getTableName()))
                    .exceptionally(e ->
                    {
                            if (shouldCreateTables && ExceptionUtils.isCauseInChain(ResourceNotFoundException.class, e))
                            {
                                final Table newTable = dynamoDB.createTable(tableName,
                                        Collections.singletonList(
                                                new KeySchemaElement("_id", KeyType.HASH)),
                                        Collections.singletonList(
                                                new AttributeDefinition("_id", ScalarAttributeType.S)),
                                        new ProvisionedThroughput(10L, 10L));

                                try
                                {
                                    newTable.waitForActive();
                                }
                                catch(Exception ex)
                                {
                                    throw new UncheckedException(ex);
                                }

                                tableHashMap.putIfAbsent(tableName, newTable);
                                return newTable;
                            }
                            else
                            {
                                throw new UncheckedException(e);
                            }
                    });
        }
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public AmazonCredentialType getCredentialType()
    {
        return credentialType;
    }

    public void setCredentialType(AmazonCredentialType credentialType)
    {
        this.credentialType = credentialType;
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getSessionToken()
    {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken)
    {
        this.sessionToken = sessionToken;
    }

    public boolean getShouldCreateTables()
    {
        return shouldCreateTables;
    }

    public void setShouldCreateTables(boolean shouldCreateTables)
    {
        this.shouldCreateTables = shouldCreateTables;
    }

}
