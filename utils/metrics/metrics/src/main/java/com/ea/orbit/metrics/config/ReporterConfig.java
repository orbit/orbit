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

package com.ea.orbit.metrics.config;

import com.ea.orbit.metrics.MetricsManager;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;

import java.util.concurrent.TimeUnit;

/**
 * Created by Jeff on 6/3/2015.
 */
public abstract class ReporterConfig
{
    private int period = 1;
    private String periodUnit = "MINUTES";
    private String rateUnit = "SECONDS";
    private String durationUnit = "MILLISECONDS";
    private String prefix = "";


    public String getPrefix()
    {
        if (!MetricsManager.getInstance().getGlobalPrefix().isEmpty())
        {
            return MetricsManager.getInstance().getGlobalPrefix() + prefix;
        }
        else
        {
            return prefix;
        }
    }

    public void setPrefix(final String prefix)
    {
        this.prefix = prefix;
    }

    public int getPeriod()
    {
        return period;
    }

    public void setPeriod(int period)
    {
        this.period = period;
    }

    public String getPeriodUnit()
    {
        return periodUnit;
    }

    public void setPeriodUnit(final String periodUnit)
    {
        this.periodUnit = periodUnit;
    }

    public String getRateUnit()
    {
        return rateUnit;
    }

    public void setRateUnit(final String rateUnit)
    {
        this.rateUnit = rateUnit;
    }

    public String getDurationUnit()
    {
        return durationUnit;
    }

    public void setDurationUnit(final String durationUnit)
    {
        this.durationUnit = durationUnit;
    }

    protected TimeUnit getRateTimeUnit()
    {
        return TimeUnit.valueOf(getRateUnit());
    }

    protected TimeUnit getDurationTimeUnit()
    {
        return TimeUnit.valueOf(getDurationUnit());
    }

    protected TimeUnit getPeriodTimeUnit()
    {
        return TimeUnit.valueOf(getPeriodUnit());
    }

    public synchronized Reporter enableReporter(MetricRegistry registry) {return null;}
}
