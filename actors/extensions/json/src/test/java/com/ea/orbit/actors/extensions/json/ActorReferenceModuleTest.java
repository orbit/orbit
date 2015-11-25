package com.ea.orbit.actors.extensions.json;

import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.Message;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ActorReferenceModuleTest
{
    public static class SomeObject
    {
        int x = 5;
    }

    @Test
    @Ignore
    public void test() throws IOException
    {
        final ActorReferenceModule actorReferenceModule = new ActorReferenceModule(DefaultDescriptorFactory.get());
        final ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(actorReferenceModule);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS, JsonTypeInfo.As.PROPERTY);

        TypeResolverBuilder<?> typer = new ClassIdTypeResolverBuilder(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
//        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);



        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json0 = mapper.writeValueAsString(new Object[]{ new Object[]{ Arrays.asList(new SomeObject()) } });
        System.out.println(json0);
        assertEquals("[{\"@type\":\"1761130608\",\"x\":5}]", json0);
        assertEquals("{\"x\":5}", mapper.writeValueAsString(new SomeObject()));

        final String json = "{ \"@type\":1761130608, \"x\": 5 }";
        final Object obj = mapper.readValue(json, Object.class);
        assertEquals(5, ((SomeObject) obj).x);
    }

    @Test
    public void testMessage() throws Exception
    {
        Message message = new Message();
        JsonMessageSerializer jsonMessageSerializer = new JsonMessageSerializer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.reset();
        message.setPayload(Arrays.asList(1, 2, 3));
        jsonMessageSerializer.serializeMessage(null, out, message);
        System.out.println(new String(out.toByteArray()));

        out.reset();
        message.setPayload(new Object[]{ 1, 2, 3 });
        jsonMessageSerializer.serializeMessage(null, out, message);
        System.out.println(new String(out.toByteArray()));

        out.reset();
        message.setPayload(new ArrayList(Arrays.asList(new Object[]{ 1, 2, 3 })));
        jsonMessageSerializer.serializeMessage(null, out, message);
        System.out.println(new String(out.toByteArray()));

    }


}
