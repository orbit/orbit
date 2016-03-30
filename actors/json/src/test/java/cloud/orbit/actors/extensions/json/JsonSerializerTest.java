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

package cloud.orbit.actors.extensions.json;

import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.Message;

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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JsonSerializerTest
{

    // {"messageType":1,"messageId":1,"headers":{},"interfaceId":769874740,"objectId":"0","methodId":-448819364,"payload":["default",1590615529,"mec:all:master","/kiQzyGrgIffiGbscC3NibD"]}

    @Test
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

        TypeResolverBuilder<?> typer = new ClassIdTypeResolverBuilder(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        {
            String str = "{\"payload\":[{\"@type\":\"-47419438\",\"payload\":5}]}";
            String str2 = "{\"payload\":[{\"@type\":-47419438,\"payload\":5}]}";
            SomeObject obj = mapper.readValue(str, SomeObject.class);
            assertEquals(str2, mapper.writeValueAsString(obj));
        }

        {
            String str = "[{\"@type\":-47419438,\"payload\":5}]";
            assertEquals(str, mapper.writeValueAsString(new Object[]{ new SomeObject(5) }));
        }

        {
            String str = "[{\"@type\":-47419438,\"payload\":5}]";
            Object obj = mapper.readValue(str, Object.class);
            assertEquals(str, mapper.writeValueAsString(obj));
        }


        {
            String str = "{\"payload\":[\"test\",{\"@type\":\"-47419438\",\"payload\":5}]}";
            String str2 = "{\"payload\":[\"test\",{\"@type\":-47419438,\"payload\":5}]}";
            SomeObject obj = mapper.readValue(str, SomeObject.class);
            assertEquals(str2, mapper.writeValueAsString(obj));
        }

        {
            String str = "{\"payload\":[\"test\",{\"@type\":\"-47419438\",\"payload\":5}],\"headers\":{}}";
            String str2 = "{\"payload\":[\"test\",{\"@type\":-47419438,\"payload\":5}],\"headers\":{}}";
            SomeObject obj = mapper.readValue(str, SomeObject.class);
            SomeObject obj2 = mapper.readValue(str, SomeObject.class);
            assertEquals(str2, mapper.writeValueAsString(obj));
            assertEquals(str2, mapper.writeValueAsString(obj2));
        }
        {
            String str = "{\"payload\":[\"test\",{\"@type\":-47419438,\"payload\":5}],\"headers\":{}}";
            SomeObject obj = mapper.readValue(str, SomeObject.class);
            assertEquals(str, mapper.writeValueAsString(obj));
        }

            assertEquals("{\"payload\":{\"@type\":-47419438,\"payload\":5}}",
                mapper.writeValueAsString(new SomeObject(new SomeObject(5))));

        mapper.readValue("{\"payload\":[\"tes\"]}", SomeObject.class);


        {
            final String json = "[[{\"@type\":\"-47419438\",\"payload\":5}]]";
            final Object obj = mapper.readValue(json, Object[].class);
        }
        {
            final String json = "[[{\"@type\":-47419438,\"payload\":5}]]";
            final Object obj = mapper.readValue(json, Object[].class);
        }



        assertEquals("[[]]",
                mapper.writeValueAsString(new Object[]{ new Object[]{} }));
        assertEquals("[[[]]]",
                mapper.writeValueAsString(new Object[]{ new Object[]{new ArrayList<Object>()} }));
        assertEquals("[[{}]]",
                mapper.writeValueAsString(new Object[]{ new Object[]{new HashMap()} }));


        assertEquals("[{\"@type\":-47419438,\"payload\":5}]",
                mapper.writeValueAsString(new Object[]{ new SomeObject(5) }));
        assertEquals("[[{\"@type\":-47419438,\"payload\":5}]]",
                mapper.writeValueAsString(new Object[]{ new Object[]{ new SomeObject(5) } }));



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

class SomeObject
{
    Object payload;

    private Map<Object, Object> headers;

    public SomeObject()
    {
    }

    public SomeObject(final Object payload)
    {
        this.payload = payload;
    }

    public Object getPayload()
    {
        return payload;
    }

    public void setPayload(final Object payload)
    {
        this.payload = payload;
    }

    public Map<Object, Object> getHeaders()
    {
        return headers;
    }

    public void setHeaders(final Map<Object, Object> headers)
    {
        this.headers = headers;
    }
}
