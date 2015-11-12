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

package com.ea.orbit.actors.ws.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.test.FakeClusterPeer;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Container;
import com.ea.orbit.web.EmbeddedHttpServer;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Server
{
    private static MixedSerializer serializer = new MixedSerializer();

    public interface HelloWebApi
    {
        @Path("/hello")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        String hello(String message);
    }

    public static class HelloWebHandler implements HelloWebApi
    {
        @Override
        public String hello(String message)
        {
            System.out.println("HelloWebHandler: " + message);
            return "helloWeb: " + message;
        }
    }

    public interface Hello extends Actor
    {
        Task<String> hello(String msg);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        public Task<String> hello(String msg)
        {
            System.out.println("HelloActor: " + msg);
            return Task.fromValue("hello: " + msg);
        }
    }

    @Singleton
    public static class SFakePeer extends FakeClusterPeer
    {

    }

    public static void main(String[] args) throws Exception
    {
        Map<String, Object> properties = new HashMap<>();

        properties.put("orbit.http.port", 9090);
        properties.put("orbit.actors.clusterName", "cluster");
        properties.put("orbit.components", Arrays.asList(
                com.ea.orbit.actors.server.ServerModule.class,
                SFakePeer.class,
                HelloActor.class,
                Hello.class,
                HelloWebHandler.class,
                EmbeddedHttpServer.class,
                MyWebSocketServer.class));

        final Container container = new Container();
        container.setProperties(properties);
        container.start();

    }
}
