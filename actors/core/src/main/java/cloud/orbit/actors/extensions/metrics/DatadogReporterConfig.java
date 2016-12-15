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

package cloud.orbit.actors.extensions.metrics;

import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.HttpTransport;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by jgong on 12/14/16.
 */
public class DatadogReporterConfig extends ReporterConfig
{

    private String host;
    private String apiKey;

    @Override
    public synchronized ScheduledReporter enableReporter(MetricRegistry registry)
    {
        final HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(apiKey).build();
        DatadogReporter datadogReporter = null;
        try
        {
            datadogReporter = DatadogReporter.forRegistry(registry).withHost(InetAddress.getLocalHost().getCanonicalHostName()).withPrefix(this.getPrefix()).withTransport(httpTransport).build();
        }
        catch (UnknownHostException e)
        {
//            e.printStackTrace();
        }

        datadogReporter.start(getPeriod(), getPeriodTimeUnit());

        return datadogReporter;
    }

    public String getHost()
    {
        return host;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }
}
