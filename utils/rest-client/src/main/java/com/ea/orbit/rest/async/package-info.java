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

/**
 * Tools to use {@ling java.util.concurrent.CompletableFuture} and {@link com.ea.orbit.concurrent.Task}
 * with jax-rs rest interfaces.
 *
 * Example:
 * <pre><code>
 * public interface Hello
 * {
 *     @GET
 *     @Path("/") CompletableFuture<String> getHome();
 * }
 *
 * public static void main(String args[])
 * {
 *     WebTarget webTarget = getWebTarget("http://example.com");
 *
 *     Hello hello = new OrbitRestClient(webTarget).get(Hello.class);
 *
 *     CompletableFuture<String> response = hello.getHome();
 *
 *     response.thenAccept(x -> System.out.println(x));
 *     response.join();
 * }
 *
 * // use any jax-rs client library to get javax.ws.rs.client.WebTarget
 * private static WebTarget getWebTarget(String host)
 * {
 *     ClientConfig clientConfig = new ClientConfig();
 *     Client client = ClientBuilder.newClient(clientConfig);
 *     return client.target(host);
 * }
 * </code></pre>
 */
package com.ea.orbit.rest.async;
