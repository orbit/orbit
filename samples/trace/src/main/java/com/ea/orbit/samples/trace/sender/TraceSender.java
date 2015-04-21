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

package com.ea.orbit.samples.trace.sender;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.providers.IInvokeListenerProvider;
import com.ea.orbit.actors.runtime.Execution;
import com.ea.orbit.samples.trace.messaging.ITraceMessaging;
import com.ea.orbit.samples.trace.messaging.TraceInfo;
import com.ea.orbit.samples.trace.messaging.TraceMulticastMessaging;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class TraceSender implements IInvokeListenerProvider
{

    private ITraceMessaging messaging = new TraceMulticastMessaging(); //default implementation

    public TraceSender()
    {
        Execution.traceEnabled = true;
    }

    Cache<Long, TraceInfo> traceMap = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public void preInvoke(long traceId, String sourceInterface, String sourceId, String targetInterface, String targetId, int methodId, Object[] params)
    {
        if (!Execution.traceEnabled)
        {
            return;
        }
        try
        {
            if (isIgnorable(sourceInterface) || isIgnorable(targetInterface))
            {
                return;
            }
            TraceInfo info = new TraceInfo();
            info.sourceInterface = sourceInterface;
            info.targetInterface = targetInterface;
            info.sourceId = sourceId;
            info.targetId = targetId;
            info.methodId = methodId;
            info.params = params;
            info.start = System.currentTimeMillis();
            traceMap.put(traceId, info);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return;
    }

    public void postInvoke(long traceId, Object result)
    {
        if (!Execution.traceEnabled)
        {
            return;
        }
        try
        {
            TraceInfo info = traceMap.getIfPresent(traceId);
            if (info != null)
            {
                traceMap.invalidate(traceId);
                if (info != null)
                {
                    info.result = result;
                    info.end = System.currentTimeMillis();
                    messaging.send(info);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean isIgnorable(String value)
    {
        if (value == null)
        {
            return true;
        }

        try
        {
            if (!IActor.class.isAssignableFrom(Class.forName(value)))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //TODO REMOTE FILTER
        //Remote filter is a good option for IO economy, but different receivers may have different filter needs. The sender filter should merge them somehow.
        return false;
    }

    public ITraceMessaging getMessaging()
    {
        return messaging;
    }

    public void setMessaging(final ITraceMessaging messaging)
    {
        this.messaging = messaging;
    }

}
