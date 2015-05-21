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

package com.ea.orbit.actors.test;

import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.RefFactory;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.actors.test.actors.SomeActor;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JsonReferenceSerializationTest
{
    private RefFactory factory = new ReferenceFactory();

    @Test
    public void testSerialize() throws Exception
    {
        String json = "\"123\"";
        SomeActor actor = factory.getReference(SomeActor.class, "123");
        ObjectMapper mapper = createMapper();
        assertEquals(json, mapper.writeValueAsString(actor));
    }

    @Test
    public void testDeserialize() throws Exception
    {
        String json = "\"123\"";
        SomeActor actor = factory.getReference(SomeActor.class, "123");
        ObjectMapper mapper = createMapper();
        assertEquals(actor, mapper.readValue(json, SomeActor.class));
    }

    @Test
    public void testList() throws Exception
    {
        String json = "[\"1\",\"2\"]";
        SomeActor actor1 = factory.getReference(SomeActor.class, "1");
        SomeActor actor2 = factory.getReference(SomeActor.class, "2");
        ObjectMapper mapper = createMapper();
        List<SomeActor> actors = Arrays.asList(actor1, actor2);
        String listJson = mapper.writeValueAsString(actors);
        assertEquals(json, listJson);
        assertEquals(actors, mapper.readValue(listJson, mapper.getTypeFactory().constructCollectionType(List.class, SomeActor.class)));
    }

    public static class ComplexData
    {
        SomeActor a1;
        SomeActor a2;
        List<SomeActor> list;

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ComplexData)) return false;

            final ComplexData that = (ComplexData) o;

            if (!a1.equals(that.a1)) return false;
            if (!a2.equals(that.a2)) return false;
            if (!list.equals(that.list)) return false;

            return true;
        }
    }

    @Test
    public void testClassWithInnerValues() throws Exception
    {
        String json = "{\"a1\":\"1\",\"a2\":\"2\",\"list\":[\"1\",\"2\"]}";
        ComplexData data = new ComplexData();
        data.a1 = factory.getReference(SomeActor.class, "1");
        data.a2 = factory.getReference(SomeActor.class, "2");
        data.list = Arrays.asList(data.a1, data.a2);
        ObjectMapper mapper = createMapper();
        assertEquals(json, mapper.writeValueAsString(data));
        assertEquals(data, mapper.readValue(json, ComplexData.class));
    }

    private ObjectMapper createMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        mapper.registerModule(new ActorReferenceModule(factory));
        return mapper;
    }
}
