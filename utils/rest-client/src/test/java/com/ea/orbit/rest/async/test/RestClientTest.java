package com.ea.orbit.rest.async.test;/*
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

import com.ea.orbit.concurrent.Task;
import com.ea.orbit.rest.async.OrbitRestClient;
import com.ea.orbit.web.EmbeddedHttpServer;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class RestClientTest
{

    private static EmbeddedHttpServer embeddedHttpServer;
    private static String host;

    public interface Hello
    {
        @Path("/home")
        @GET
        String getHome();

        @Path("/home")
        @GET
        Task<String> getHomeAsync();

        @Path("/home")
        @GET
        CompletableFuture<String> getHomeAsyncCF();

        @Path("/home")
        @GET
        CompletionStage<String> getHomeAsyncCS();

        @Path("/params/{p1}")
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        Task<SomeResponseDto> getParams(
                @PathParam("p1") String p1,
                @HeaderParam("h1") String h1,
                @QueryParam("q1") String q1,
                @DefaultValue("oo") @QueryParam("q2") String q2,
                MessageDto message);


        @Path("/pathParam/{p1}")
        @GET
        Task<String> getPathParam(@PathParam("p1") String p1);
    }

    public static class MessageDto
    {
        public String data;
        public int x;
        public List<String> list;
    }

    public static class SomeResponseDto
    {
        public MessageDto original;
        public String resp;
    }

    @Path("/")
    public static class HelloImpl
    {
        @Path("/home")
        @GET
        public String getHome()
        {
            return "home";
        }

        @Path("/pathParam/{p1}")
        @GET
        public String getPathParam(@PathParam("p1") String p1)
        {
            return "::" + p1;
        }

        @Path("/params/{p1}")
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public SomeResponseDto getParams(
                @PathParam("p1") String p1,
                @HeaderParam("h1") String h1,
                @QueryParam("q1") String q1,
                @DefaultValue("oo") @QueryParam("q2") String q2,
                MessageDto message)
        {
            final SomeResponseDto response = new SomeResponseDto();
            response.original = message;
            response.resp = p1 + ":" + h1 + ":" + q1 + ":" + q2 + ":" + message.data;
            return response;
        }
    }

    @Test
    public void testBasicAsync() throws ExecutionException, InterruptedException
    {
        WebTarget webTarget = getWebTarget();
        final Future<Response> responseFuture = webTarget.path("/home")
                .request().async()
                .get(new InvocationCallback<Response>()
                {
                    @Override
                    public void completed(Response response)
                    {
                        System.out.println("Response status code "
                                + response.getStatus() + " received.");

                    }

                    @Override
                    public void failed(Throwable throwable)
                    {
                        System.out.println("Invocation failed.");
                        throwable.printStackTrace();
                    }
                });


        assertNotNull(responseFuture.get());
    }

    @Test
    public void testNormalProxy()
    {
        WebTarget webTarget = getWebTarget();

        final Hello hello = WebResourceFactory.newResource(Hello.class, webTarget);
        assertNotNull(hello.getHome());
    }

    @Test
    public void testSimpleGet()
    {
        WebTarget webTarget = getWebTarget();

        final Hello hello = new OrbitRestClient(webTarget).get(Hello.class);

        assertEquals("home", hello.getHome());
    }

    @Test
    public void testParams()
    {
        WebTarget webTarget = getWebTarget();

        final Hello hello = new OrbitRestClient(webTarget).get(Hello.class);

        final MessageDto message = new MessageDto();
        message.data = "data0";
        message.x = 10;
        message.list = Arrays.asList("a", "b", "c");
        final Task<SomeResponseDto> resp = hello.getParams("pp", "xx", "qq", null, message);
        final SomeResponseDto r0 = resp.join();
        assertEquals("pp:xx:qq:oo:data0", r0.resp);
        assertEquals("data0", r0.original.data);
        assertEquals("b", r0.original.list.get(1));
        assertEquals(10, r0.original.x);
    }

    @Test
    public void testPathParam()
    {
        WebTarget webTarget = getWebTarget();

        final Hello hello = new OrbitRestClient(webTarget).get(Hello.class);
        assertEquals("::xx", hello.getPathParam("xx").join());

    }

    private WebTarget getWebTarget()
    {
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        return client.target(host);
    }


    @BeforeClass
    public static void beforeClass()
    {
        embeddedHttpServer = new EmbeddedHttpServer();
        embeddedHttpServer.registerProviders(Arrays.asList(HelloImpl.class));
        embeddedHttpServer.setPort(0);
        embeddedHttpServer.start().join();
        host = "http://localhost:" + embeddedHttpServer.getLocalPort();
    }

    @AfterClass
    public static void afterClass()
    {
        embeddedHttpServer.stop();
    }
}
