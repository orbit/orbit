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

package com.ea.orbit.actors.extensions.metrics;

import com.ea.orbit.actors.extensions.NamedPipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.metrics.MetricsManager;
import com.ea.orbit.metrics.annotations.ExportMetric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by Jeff on 12/1/2015.
 */
public class MetricsPipelineExtension extends NamedPipelineExtension
{
    public static final String METRICS_PIPELINE_NAME = "metrics-messaging-pipeline";

    private Logger logger = LoggerFactory.getLogger(MetricsPipelineExtension.class);

    private LongAdder messagesReceived = new LongAdder();
    private LongAdder messagesSent = new LongAdder();

    private Map<Integer, Meter> receiveMeters = new HashMap<>();
    private Map<Integer, Meter> sendMeters = new HashMap<>();

    public MetricsPipelineExtension()
    {
        super(METRICS_PIPELINE_NAME, null, DefaultHandlers.MESSAGING);
        setupMetrics();
    }

    public MetricsPipelineExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
        setupMetrics();
    }

    private void setupMetrics()
    {
        setupMeterForMessageType(receiveMeters, "one-way-message-receive-rate", (int)MessageDefinitions.ONE_WAY_MESSAGE);
        setupMeterForMessageType(receiveMeters, "request-message-receive-rate", (int)MessageDefinitions.REQUEST_MESSAGE);
        setupMeterForMessageType(receiveMeters, "response-error-receive-rate", (int)MessageDefinitions.RESPONSE_ERROR);
        setupMeterForMessageType(receiveMeters, "response-ok-receive-rate", (int)MessageDefinitions.RESPONSE_OK);
        setupMeterForMessageType(receiveMeters, "response-protocol-error-receive-rate", (int)MessageDefinitions.RESPONSE_PROTOCOL_ERROR);
        setupMeterForMessageType(sendMeters, "one-way-message-send-rate", (int)MessageDefinitions.ONE_WAY_MESSAGE);
        setupMeterForMessageType(sendMeters, "request-message-send-rate", (int)MessageDefinitions.REQUEST_MESSAGE);
        setupMeterForMessageType(sendMeters, "response-error-send-rate", (int)MessageDefinitions.RESPONSE_ERROR);
        setupMeterForMessageType(sendMeters, "response-ok-send-rate", (int)MessageDefinitions.RESPONSE_OK);
        setupMeterForMessageType(sendMeters, "response-protocol-error-send-rate", (int)MessageDefinitions.RESPONSE_PROTOCOL_ERROR);

        MetricsManager.getInstance().registerExportedMetrics(this);
    }

    private void setupMeterForMessageType(Map<Integer, Meter> meters, String name, int messageType)
    {
        Meter meter = new Meter();
        meters.put(messageType, meter);
        MetricsManager.getInstance().registerMetric(name, meter);
    }

    @Override
    public void onRead(HandlerContext ctx, Object message)
    {
        if (message instanceof Message)
        {
            messagesReceived.increment();

            Message msg = (Message) message;
            final int messageType = msg.getMessageType();
            Meter messageMeter = receiveMeters.get(messageType);

            if (messageMeter != null)
            {
                messageMeter.mark();
            }
        }

        ctx.fireRead(message);
    }

    @Override
    public Task write(HandlerContext ctx, Object message) throws Exception
    {
        if (message instanceof Message)
        {
            messagesSent.increment();

            Message msg = (Message) message;
            final int messageType = msg.getMessageType();
            Meter messageMeter = sendMeters.get(messageType);

            if (messageMeter != null)
            {
                messageMeter.mark();
            }
        }

        return ctx.write(message);
    }

    @ExportMetric(name="messagesReceived")
    public int getMessagesReceivedCount()
    {
        return messagesReceived.intValue();
    }

    @ExportMetric(name="messagesSent")
    public int getMessagesSentCount()
    {
        return messagesSent.intValue();
    }
}
