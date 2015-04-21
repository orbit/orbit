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

package com.ea.orbit.samples.trace.demo;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.samples.trace.messaging.TraceMulticastMessaging;
import com.ea.orbit.samples.trace.receiver.TraceReceiver;
import com.ea.orbit.samples.trace.receiver.filter.GlobTraceFilter;
import com.ea.orbit.samples.trace.receiver.view.DiagramTraceView;
import com.ea.orbit.samples.trace.receiver.view.RealtimeGraphView;
import com.ea.orbit.samples.trace.receiver.view.TableTraceView;
import com.ea.orbit.samples.trace.sender.TraceSender;

public class TraceDemoMain
{

    public static void main(String[] args) throws Exception
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        OrbitStage stage = new OrbitStage();
        stage.setClusterName("traceCluster" + Math.random());

        //MESSAGING
        TraceMulticastMessaging messaging = new TraceMulticastMessaging();
        messaging.setGroupAddress(randomGroupAddress());
        messaging.setPort(8888);
        messaging.start();

        //SENDER
        TraceSender sender = new TraceSender();
        sender.setMessaging(messaging);
        stage.addProvider(sender);

        //RECEIVER
        TraceReceiver receiver = new TraceReceiver();
        receiver.setMessaging(messaging);
        receiver.setFilter(new GlobTraceFilter(800, 0, 400, 80));
        receiver.addView(new RealtimeGraphView(800, 80, 1024, 300));
        receiver.addView(new TableTraceView(800, 410, 1024, 200));
        receiver.addView(new DiagramTraceView("./samples/trace/graph.css"));
        //receiver.addView(new TextTraceView());
        receiver.start();

        //start some cluster activity
        stage.start().join();
        IExampleA first = IActor.getReference(IExampleA.class, "0");
        first.someWork().join();

    }

    public static String randomGroupAddress()
    {
        //multicast range is from 224.0.0.0 to 239.255.255.255
        return "" + (224 + (int) (Math.random() * 15)) + "." + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255);
    }

}

