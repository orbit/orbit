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

package com.ea.orbit.metrics;

import com.ea.orbit.annotation.Config;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.metrics.config.ReporterConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MetricsComponent implements Startable
{
    private static final Logger logger = LoggerFactory.getLogger(MetricsComponent.class);

    @Config("orbit.metrics.runtimeIdentifier")
    private String runtimeIdentifier = "";

    @Config("orbit.metrics.reporters")
    private List<ReporterConfig> metricsConfig = new ArrayList<>();

    @Override
    public Task<?> start()
    {
        try
        {
            Class.forName("com.ea.orbit.metrics.MetricsManager"); //make sure the class is in the classpath.
            String cleanRuntimeId = MetricsManager.sanitizeMetricName(runtimeIdentifier);
            MetricsManager.getInstance().initializeMetrics(cleanRuntimeId, metricsConfig);
        }
        catch(Exception ex)
        {
            logger.info("Skipping Orbit Metrics initialization because it was not found in the classpath.");
        }

        return Task.done();
    }

    @Override
    public Task<?> stop()
    {
        return Task.done();
    }
}
