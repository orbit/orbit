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

package cloud.orbit.actors.test.serialization;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.json.ActorReferenceModule;
import cloud.orbit.actors.runtime.DescriptorFactory;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JsonReferenceSerializationTest
{
    private DescriptorFactory factory = DefaultDescriptorFactory.get();

    public static class RefHolder {
        SomeActor actor ;
    }
    @Test
    public void testSerialize() throws Exception
    {
        String json = "{\"actor\":\"123\"}";
        SomeActor actor = factory.getReference(SomeActor.class, "123");
        RefHolder ref = new RefHolder();
        ref.actor = actor;
        ObjectMapper mapper = createMapper();
        assertEquals(json, mapper.writeValueAsString(ref));
    }

    @Test(expected = JsonMappingException.class)
    public void testInvalidSerialize() throws Exception
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

    public static class ListHolder {
        List<SomeActor> actors;
    }

    @Test
    public void testList() throws Exception
    {
        String json = "{\"actors\":[\"1\",\"2\"]}";
        SomeActor actor1 = factory.getReference(SomeActor.class, "1");
        SomeActor actor2 = factory.getReference(SomeActor.class, "2");
        ObjectMapper mapper = createMapper();
        ListHolder holder = new ListHolder();
        holder.actors = Arrays.asList(actor1, actor2);
        String listJson = mapper.writeValueAsString(holder);
        assertEquals(json, listJson);
        assertEquals(holder.actors, mapper.readValue(listJson, ListHolder.class).actors);
    }

    @Test(expected = JsonMappingException.class)
    public void testInvalidList() throws Exception
    {
        SomeActor actor1 = factory.getReference(SomeActor.class, "1");
        SomeActor actor2 = factory.getReference(SomeActor.class, "2");
        ObjectMapper mapper = createMapper();
        final List<SomeActor> actors = Arrays.asList(actor1, actor2);
        mapper.writeValueAsString(actors);
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

    public static class Data1
    {
        public SomeActor ref;
        public Actor ref2;
        public SomeActorBaseActor ref3;
        public Set<SomeActor> set1;
        public Set<Actor> set2;
        public Set<SomeActorBaseActor> set3;
    }

    public static class Data2
    {
        public Object ref;
    }

    public interface SomeActorBaseActor extends Actor
    {

    }

    public interface SomeActor extends Actor, SomeActorBaseActor
    {

    }

    @Test
    public void testMultipleTypesOfRef() throws IOException
    {
        final Data1 data = new Data1();
        final SomeActor ref = Actor.getReference(SomeActor.class, "a");
        data.ref = ref;
        data.ref2 = ref;
        data.ref3 = ref;
        data.set1 = Collections.singleton(ref);
        data.set2 = Collections.singleton(ref);
        data.set3 = Collections.singleton(ref);

        final ObjectMapper mapper = createMapper();

        final String str = mapper.writeValueAsString(data);
        final Data1 data2 = mapper.readValue(str, Data1.class);
        assertEquals(data.ref, data2.ref);
        assertEquals(data.ref2, data2.ref2);
        assertEquals(data.ref3, data2.ref3);
        assertEquals(data.set1, data2.set1);
        assertEquals(data.set2, data2.set2);
        assertEquals(data.set3, data2.set3);


        final Map raw = mapper.readValue(str, LinkedHashMap.class);
        assertEquals("a", raw.get("ref").toString());
        assertEquals("!!cloud.orbit.actors.test.serialization.JsonReferenceSerializationTest$SomeActor a", raw.get("ref2").toString());
        assertEquals("!!cloud.orbit.actors.test.serialization.JsonReferenceSerializationTest$SomeActor a", raw.get("ref3").toString());

        assertEquals("[a]", raw.get("set1").toString());
        assertEquals("[!!cloud.orbit.actors.test.serialization.JsonReferenceSerializationTest$SomeActor a]", raw.get("set2").toString());
        assertEquals("[!!cloud.orbit.actors.test.serialization.JsonReferenceSerializationTest$SomeActor a]", raw.get("set3").toString());

    }

    @Test(expected = JsonMappingException.class)
    public void testIllegalRef() throws IOException
    {
        final Data2 data = new Data2();
        final SomeActor ref = Actor.getReference(SomeActor.class, "a");
        data.ref = ref;

        final ObjectMapper mapper = createMapper();

        final String str = mapper.writeValueAsString(data);
        final Data2 data2 = mapper.readValue(str, Data2.class);
        assertEquals(data.ref, data2.ref);
    }

    private ObjectMapper createMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        mapper.registerModule(new ActorReferenceModule(factory));
        return mapper;
    }
}
