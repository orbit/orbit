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

package com.ea.orbit.web.test;

import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import javax.inject.Singleton;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("/test")
public class Hello
{
    public static class HelloResult
    {
        private int helloCount;

        public int getHelloCount()
        {
            return helloCount;
        }

        public void setHelloCount(int helloCount)
        {
            this.helloCount = helloCount;
        }
    }

    private int count = 0;

    public Hello()
    {
        count = 0;
    }

    @GET
    @Path("/helloRaw")
    @Produces(MediaType.APPLICATION_JSON)
    public HelloResult getHelloRaw()
    {
        HelloResult result = new HelloResult();
        result.setHelloCount(++count);
        return result;
    }

    @GET
    @Path("/helloTask")
    @Produces(MediaType.APPLICATION_JSON)
    public Task<HelloResult> getHelloTask()
    {
        HelloResult result = new HelloResult();
        result.setHelloCount(++count);
        return Task.fromValue(result);
    }

    @GET
    @Path("/serverErrorRaw")
    @Produces(MediaType.APPLICATION_JSON)
    public HelloResult getServerErrorRaw()
    {
        throw new UncheckedException("serverError");
    }

    @GET
    @Path("/serverErrorTask")
    @Produces(MediaType.APPLICATION_JSON)
    public Task<HelloResult> getServerErrorTask()
    {
        throw new UncheckedException("serverError");
    }

    @GET
    @Path("/serverErrorNestedTask")
    @Produces(MediaType.APPLICATION_JSON)
    public Task<HelloResult> getServerErrorNestedTask()
    {
        return Task.supplyAsync(() -> {throw new UncheckedException("serverError"); });

    }

    @GET
    @Path("/forbiddenRaw")
    @Produces(MediaType.APPLICATION_JSON)
    public HelloResult getForbiddenRaw()
    {
        throw new ForbiddenException("forbidden");
    }

    @GET
    @Path("/forbiddenTask")
    @Produces(MediaType.APPLICATION_JSON)
    public Task<HelloResult> getForbiddenTask()
    {
        return Task.supplyAsync(() -> {throw new ForbiddenException("forbidden"); });
    }

    @GET
    @Path("/listTask")
    @Produces(MediaType.APPLICATION_JSON)
    public Task<List<HelloResult>> getListTask()
    {
        List<HelloResult> results = new ArrayList<>();
        HelloResult helloResult = new HelloResult();
        helloResult.setHelloCount(++count);
        results.add(helloResult);
        helloResult = new HelloResult();
        helloResult.setHelloCount(++count);
        results.add(helloResult);
        
        return Task.fromValue(results);
    }


}