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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.concurrent.Task;

import java.lang.reflect.Method;
import java.util.Map;

public class Invocation
{
    private RemoteReference toReference;
    private Method method;
    private boolean oneWay;
    private int methodId;
    private Object[] params;
    private Map<String, Object> headers;
    private Task completion;
    private NodeAddress toNode;
    private NodeAddress fromNode;
    private int hops;
    private int messageId;

    public Invocation()
    {
    }

    public Invocation(final RemoteReference toReference, final Method method, final boolean oneWay, final int methodId, final Object[] params, final Task completion)
    {
        super();
        this.toReference = toReference;
        this.method = method;
        this.oneWay = oneWay;
        this.methodId = methodId;
        this.params = params;
        this.completion = completion;
    }

    public RemoteReference getToReference()
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

    public Map<String, Object> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers)
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
        return getToReference() + "." + (method != null ? method.getName() : Integer.toString(methodId));
    }

    public int getHops()
    {
        return hops;
    }

    public void setHops(final int hops)
    {
        this.hops = hops;
    }

    public void setCompletion(final Task completion)
    {
        this.completion = completion;
    }

    public int getMessageId()
    {
        return messageId;
    }

    public void setMessageId(final int messageId)
    {
        this.messageId = messageId;
    }

    public void setFromNode(final NodeAddress fromNode)
    {
        this.fromNode = fromNode;
    }

    public Invocation withToReference(final RemoteReference toReference)
    {
        this.toReference = toReference;
        return this;
    }

    public Invocation withMethod(final Method method)
    {
        this.method = method;
        return this;
    }

    public Invocation withOneWay(final boolean oneWay)
    {
        this.oneWay = oneWay;
        return this;
    }

    public Invocation withMethodId(final int methodId)
    {
        this.methodId = methodId;
        return this;
    }

    public Invocation withParams(final Object[] params)
    {
        this.params = params;
        return this;
    }

    public Invocation withHeaders(final Map<String, Object> headers)
    {
        this.headers = headers;
        return this;
    }
}
