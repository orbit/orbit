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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whalin.MemCached.MemCachedClient;

import java.io.IOException;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public class MemCachedStorageHelper
{

    public static final String KEY_SEPARATOR = "|";

    private static final Logger logger = LoggerFactory.getLogger(MemCachedStorageProvider.class);

    private final MemCachedClient memCachedClient;

    public MemCachedStorageHelper(final MemCachedClient memCachedClient)
    {
        this.memCachedClient = memCachedClient;
    }

    public Object get(String key) {
        return deserialize(key, getMemcachedValueAsBytes(key));
    }

    public void set(String key, Object value) {
        memCachedClient.set(key, serialize(value));
    }

    public byte[] serialize(Object value) {
        return (value != null) ? ByteUtils.serializeObject(value) : null;
    }

    @SuppressWarnings("unchecked")
    private Object deserialize(String key, byte[] serializedValue) {
        if(serializedValue != null) {
            try {
                return ByteUtils.getSerializedObject(serializedValue);
            } catch (IOException ioe) {
                logger.info("IOException during cache value deserialization for key: " + key + " - removing entry");
            } catch (ClassNotFoundException cnfe) {
                logger.info("ClassNotFoundException during cache value deserialization for key: " + key + " - removing entry");
            }

            // Remove the entry upon error deserializing its value.
            memCachedClient.delete(key);
        }

        return null;
    }

    private byte[] getMemcachedValueAsBytes(String key) {
        // Though the source code is unclear, testing shows that com.meetup.memcached.MemcachedClient will always fetch
        // and return a stored byte[] as a byte[] and not a String.
        return (byte[]) memCachedClient.get(key);
    }
}
