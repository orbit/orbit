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

import com.ea.orbit.metrics.annotations.ExportMetric;
import com.ea.orbit.metrics.config.ReporterConfig;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MetricsManager
{
    private static MetricsManager instance = new MetricsManager();
    private static final MetricRegistry registry = new MetricRegistry();
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    private Map<ReporterConfig, ScheduledReporter> reporters = new HashMap<>();
    private boolean isInitialized = false;

    protected MetricsManager()
    {

    }

    public static synchronized MetricsManager getInstance()
    {
        if (instance == null)
        {
            instance = new MetricsManager();
        }
        return instance;
    }

    public static String sanitizeMetricName(String name)
    {
        return name.replaceAll("[\\[\\]\\.\\\\/]", ""); //strip illegal characters
    }


    public synchronized void initializeMetrics(List<ReporterConfig> reporterConfigs)
    {
        if (!isInitialized)
        {
            for (ReporterConfig reporterConfig : reporterConfigs)
            {
                ScheduledReporter reporter = reporterConfig.enableReporter(registry);
                if (reporter != null)
                {
                    reporters.put(reporterConfig, reporter);
                }
                else
                {
                    logger.warn("Failed to enable reporter " + reporterConfig.getClass().getName());
                }
            }
            isInitialized = true;
        }
        else
        {
            logger.info("Attempting to initialize the Metrics Manager when it is already initialized!");
        }
    }

    public void registerExportedMetrics(Object obj)
    {
        if (obj == null)
        {
            return;
        }

        for (Field field : findFieldsForMetricExport(obj))
        {
            final ExportMetric annotation = field.getAnnotation(ExportMetric.class);
            //check to see if the field is accessible.
            if (!field.isAccessible())
            {
                throw new IllegalStateException("Field " + field.getName() + " in object " + obj.getClass().getName() + " is marked for Metrics Export but the field is not accessible");
            }

            registerGauge(annotation, obj, () -> {
                try
                {
                    Object value = field.get(obj);
                    return value;
                }
                catch (IllegalAccessException iae) //convert to an unchecked exception.
                {
                    throw new IllegalStateException("Field " + field.getName() + " was inaccessible: " + iae.getMessage());
                }
            });
        }

        for (Method method : findMethodsForMetricExport(obj))
        {
            //methods declared as metrics must not accept parameters.
            if (method.getParameterCount() > 0)
            {
                throw new IllegalArgumentException("Method " + method.getName() + " in object " + obj.getClass().getName() + " is marked for Metrics Export but the method definition contains parameters.");
            }

            final ExportMetric annotation = method.getAnnotation(ExportMetric.class);

            registerGauge(annotation, obj, () -> {
                try
                {
                    Object value = method.invoke(obj, new Object[0]);
                    return value;
                }
                catch (IllegalAccessException iae) //convert to an unchecked exception.
                {
                    throw new IllegalStateException("Method " + method.getName() + " was inaccessible: " + iae.getMessage());
                }
                catch (InvocationTargetException ite)
                {
                    throw new UncheckedException("Invocation of method " + method.getName() + " failed", ite.getTargetException());
                }
            });
        }
    }

    public void unregisterExportedMetrics(Object obj)
    {
        if (obj == null)
        {
            return;
        }

        for (Field field : findFieldsForMetricExport(obj))
        {
            final ExportMetric annotation = field.getAnnotation(ExportMetric.class);

            String metricName = buildMetricName(obj, annotation);
            registry.remove(metricName);
        }

        for (Method method : findMethodsForMetricExport(obj))
        {
            final ExportMetric annotation = method.getAnnotation(ExportMetric.class);

            String metricName = buildMetricName(obj, annotation);
            registry.remove(metricName);
        }
    }

    private void registerGauge(ExportMetric annotation, Object obj, Supplier<Object> metricSupplier)
    {
        final String gaugeName = buildMetricName(obj.getClass(), annotation);
        try
        {
            registry.register(gaugeName, (Gauge<Object>) () -> {

                Object value = metricSupplier.get();
                return value;
            });

            if (logger.isDebugEnabled())
            {
                logger.debug("Registered new metric " + annotation.name());
            }
        }
       catch (IllegalArgumentException iae)
       {
           logger.warn("Unable to register metric " + annotation.name() + " because a metric already has been registered with the id: " + gaugeName);
       }
    }

    private List<Field> findFieldsForMetricExport(Object obj)
    {
        List<Field> exportedFields = new ArrayList<>();

        if (obj != null)
        {
            for (Field field : obj.getClass().getDeclaredFields())
            {
                if (field.isAnnotationPresent(ExportMetric.class))
                {
                    exportedFields.add(field);
                }
            }
        }

        return exportedFields;
    }

    private List<Method> findMethodsForMetricExport(Object obj)
    {
        List<Method> exportedMethods = new ArrayList<>();

        if (obj != null)
        {
            for (Method method : obj.getClass().getDeclaredMethods())
            {
                if (method.isAnnotationPresent(ExportMetric.class))
                {
                    exportedMethods.add(method);
                }
            }
        }

        return exportedMethods;
    }

    private String buildMetricName(Object obj, ExportMetric annotation)
    {
        return MetricRegistry.name(obj.getClass(), annotation.name());
    }
}
