package com.ea.orbit.metrics.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jefft on 9/21/2015.
 */
public class ThreadCPUUsageSet implements MetricSet
{

    private final ThreadMXBean threads;

    public ThreadCPUUsageSet()
    {
        this(ManagementFactory.getThreadMXBean());
    }

    public ThreadCPUUsageSet(ThreadMXBean threads)
    {
        this.threads = threads;
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();

        ThreadInfo[] threadInfos = threads.getThreadInfo(threads.getAllThreadIds());
        for (final ThreadInfo info : threadInfos)
        {
            gauges.put("Thread." + info.getThreadName() + ".cputime", new Gauge<Long>()
            {
                @Override
                public Long getValue()
                {
                    return threads.getThreadCpuTime(info.getThreadId());
                }
            });
        }
        return gauges;
    }
}
