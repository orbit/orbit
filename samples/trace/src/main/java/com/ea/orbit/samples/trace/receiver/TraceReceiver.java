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

package com.ea.orbit.samples.trace.receiver;

import com.ea.orbit.samples.trace.messaging.ITraceMessaging;
import com.ea.orbit.samples.trace.messaging.TraceInfo;
import com.ea.orbit.samples.trace.messaging.TraceMulticastMessaging;
import com.ea.orbit.samples.trace.receiver.filter.ITraceFilter;
import com.ea.orbit.samples.trace.receiver.view.ITraceView;

import java.util.ArrayList;
import java.util.List;

public class TraceReceiver
{

    public static final int TRACE_STEP_MILLIS = 100;

    private Thread thread;
    private ITraceMessaging messaging = new TraceMulticastMessaging(); //default implementation

    private ITraceFilter filter = new ITraceFilter()
    {
    };

    private List<ITraceView> views = new ArrayList<>();

    public void addView(ITraceView view)
    {
        views.add(view);
    }

    public void start()
    {
        thread = new Thread(() -> {
            while (true)
            {
                TraceInfo info = messaging.receive();
                while (info != null)
                {
                    for (ITraceView l : views)
                    {
                        try
                        {
                            if (filter.allows(info))
                            {
                                l.trace(info);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    info = messaging.receive();
                }
                try
                {
                    Thread.sleep(TraceReceiver.TRACE_STEP_MILLIS);
                }
                catch (InterruptedException e)
                {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void stop()
    {
        thread.interrupt();
    }

    public ITraceMessaging getMessaging()
    {
        return messaging;
    }

    public void setMessaging(final ITraceMessaging messaging)
    {
        this.messaging = messaging;
    }

    public ITraceFilter getFilter()
    {
        return filter;
    }

    public void setFilter(final ITraceFilter filter)
    {
        this.filter = filter;
    }
}
