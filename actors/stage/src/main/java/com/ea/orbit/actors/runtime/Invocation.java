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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.concurrent.Task;

import java.lang.reflect.Method;
import java.util.Map;

public class Invocation
{
    private final Addressable toReference;
    private final Method method;
    private final boolean oneWay;
    private final int methodId;
    private final Object[] params;
    private Map<?, ?> headers;
    private final Task completion;
    private NodeAddress toNode;
    private NodeAddress fromNode;

    public Invocation(final Addressable toReference, final Method method, final boolean oneWay, final int methodId, final Object[] params, final Task completion)
    {
        super();
        this.toReference = toReference;
        this.method = method;
        this.oneWay = oneWay;
        this.methodId = methodId;
        this.params = params;
        this.completion = completion;
    }

    public Addressable getToReference()
    {
        return toReference;
    }

    public Method getMethod()
    {
        return method;
    }

    public boolean isOneWay()
    {
        return oneWay;
    }

    public int getMethodId()
    {
        return methodId;
    }

    public Object[] getParams()
    {
        return params;
    }

    public Task getCompletion()
    {
        return completion;
    }

    public Map<?, ?> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Map<?, ?> headers)
    {
        this.headers = headers;
    }

    public NodeAddress getToNode()
    {
        return toNode;
    }

    public void setToNode(final NodeAddress toNode)
    {
        this.toNode = toNode;
    }

    public Invocation withToNode(final NodeAddress toNode)
    {
        this.toNode = toNode;
        return this;
    }

    public Invocation withFromNode(final NodeAddress fromNode)
    {
        this.fromNode = fromNode;
        return this;
    }

    public NodeAddress getFromNode()
    {
        return fromNode;
    }

    @Override
    public String toString()
    {
        return getToReference() + "." + method.getName();
    }
}