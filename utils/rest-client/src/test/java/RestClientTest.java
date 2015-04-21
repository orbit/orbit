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

import com.ea.orbit.concurrent.Task;
import com.ea.orbit.rest.async.OrbitRestClient;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;


public class RestClientTest
{

    public interface Hello
    {
        @Path("/")
        @GET
        String getHome();

        @Path("/")
        @GET
        Task<String> getHomeAsync();

        @Path("/")
        @GET
        CompletableFuture<String> getHomeAsyncCF();

        @Path("/")
        @GET
        CompletionStage<String> getHomeAsyncCS();
    }


    @Test
    public void testBasicAsync() throws ExecutionException, InterruptedException
    {
        ClientConfig clientConfig = new ClientConfig();

        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget webTarget = client.target("http://example.com");
        final Future<Response> responseFuture = webTarget.path("/")
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
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target("http://example.com");

        final Hello hello = WebResourceFactory.newResource(Hello.class, webTarget);
        assertNotNull(hello.getHome());
    }

    @Test
    public void testOrbit()
    {
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target("http://example.com");

        final Hello hello = new OrbitRestClient(webTarget).get(Hello.class);
        assertNotNull(hello.getHomeAsyncCF());
        assertNotNull(hello.getHomeAsyncCF().join());
    }

}
