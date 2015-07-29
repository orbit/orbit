package com.ea.orbit.actor.extensions.json;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.extensions.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ReferenceFactory;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JsonReferenceSerializationTest
{

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup()
    {
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));

        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
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
        assertEquals("!!com.ea.orbit.actor.extensions.json.JsonReferenceSerializationTest$SomeActor a", raw.get("ref2").toString());
        assertEquals("!!com.ea.orbit.actor.extensions.json.JsonReferenceSerializationTest$SomeActor a", raw.get("ref3").toString());

        assertEquals("[a]", raw.get("set1").toString());
        assertEquals("[!!com.ea.orbit.actor.extensions.json.JsonReferenceSerializationTest$SomeActor a]", raw.get("set2").toString());
        assertEquals("[!!com.ea.orbit.actor.extensions.json.JsonReferenceSerializationTest$SomeActor a]", raw.get("set3").toString());

    }

    @Test(expected = JsonMappingException.class)
    public void testIllegalRef() throws IOException
    {
        final Data2 data = new Data2();
        final SomeActor ref = Actor.getReference(SomeActor.class, "a");
        data.ref = ref;

        final String str = mapper.writeValueAsString(data);
        final Data2 data2 = mapper.readValue(str, Data2.class);
        assertEquals(data.ref, data2.ref);
    }
}
