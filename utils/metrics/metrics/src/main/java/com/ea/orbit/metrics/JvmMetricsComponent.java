package com.ea.orbit.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.container.Startable;
import com.ea.orbit.metrics.jvm.ThreadCPUUsageSet;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Created by jefft on 9/18/2015.
 */
@Singleton
public class JvmMetricsComponent implements Startable
{

    GarbageCollectorMetricSet gcMetricSet = new GarbageCollectorMetricSet();
    MemoryUsageGaugeSet memoryMetricSet = new MemoryUsageGaugeSet();
    ThreadStatesGaugeSet threadsMetricSet = new ThreadStatesGaugeSet();
    ThreadCPUUsageSet threadCPUMetricSet = new ThreadCPUUsageSet();

    @Override
    public Task<?> start()
    {
        MetricRegistry registry = MetricsManager.getInstance().getRegistry();
        try
        {
            registry.registerAll(gcMetricSet);
            registry.registerAll(memoryMetricSet);
            registry.registerAll(threadsMetricSet);
            registry.registerAll(threadCPUMetricSet);
        }
        catch(IllegalArgumentException iae)
        {
            //This is actually ok. This means that the metrics are already registered.
        }

        return Task.done();
    }

    @Override
    public Task<?> stop()
    {
        MetricRegistry registry = MetricsManager.getInstance().getRegistry();
        unregisterMetricSet(gcMetricSet, registry);
        unregisterMetricSet(memoryMetricSet, registry);
        unregisterMetricSet(threadsMetricSet, registry);
        unregisterMetricSet(threadCPUMetricSet, registry);
        return Task.done();
    }

    private void unregisterMetricSet(MetricSet metricSet, MetricRegistry registry)
    {
        Map<String, Metric> metrics = metricSet.getMetrics();
        metrics.entrySet().stream().map(Map.Entry::getKey).forEach(metricName -> registry.remove(metricName));
    }
}
