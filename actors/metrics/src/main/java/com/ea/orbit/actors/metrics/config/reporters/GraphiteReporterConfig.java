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

package com.ea.orbit.actors.metrics.config.reporters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import java.net.InetSocketAddress;

public class GraphiteReporterConfig extends ReporterConfig
{
    private static final Logger logger = LoggerFactory.getLogger(GraphiteReporterConfig.class);
    private String host;
    private int port;

    public String getHost()
    {
        return host;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    @Override
    public void enableReporter(MetricRegistry registry, String runtimeId)
    {
        String uniquePrefix = buildUniquePrefix(runtimeId);
        logger.info("Registering with Graphite under prefix: " + uniquePrefix);
        final Graphite graphite = new Graphite(new InetSocketAddress(host, port));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                .convertRatesTo(getRateTimeUnit())
                .convertDurationsTo(getDurationTimeUnit())
                .prefixedWith(uniquePrefix)
                .build(graphite);

        reporter.start(getPeriod(), getPeriodTimeUnit());
    }
}
