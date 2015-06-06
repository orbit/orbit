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

package com.ea.orbit.actors.metrics;

import com.ea.orbit.actors.metrics.annotations.ExportMetric;
import com.ea.orbit.actors.metrics.config.reporters.ReporterConfig;
import com.ea.orbit.annotation.Config;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Supplier;

@Singleton
public class MetricsManager
{
    static final MetricRegistry registry = new MetricRegistry();
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    private boolean isInitialized = false;

    @Config("orbit.metrics.reporters")
    private List<ReporterConfig> reporterConfigs;


    public synchronized void initializeMetrics(String uniqueId)
    {
        if (!isInitialized && reporterConfigs != null)
        {
            for (ReporterConfig reporterConfig : reporterConfigs)
            {
                reporterConfig.enableReporter(registry, uniqueId);
            }
            isInitialized = true;
        }
    }

    public <T> void registerMetric(Class clazz, String name, Supplier<T> value)
    {
        registry.register(MetricRegistry.name(clazz, name), (Gauge<T>) () -> value.get());
    }

    public void registerExportedMetrics(Object obj)
    {
        if (obj == null)
        {
            return;
        }

        for (Field field : obj.getClass().getDeclaredFields())
        {
            if (field.isAnnotationPresent(ExportMetric.class))
            {
                //check to see if we need to make the field accessible.
                if (!field.isAccessible() && (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier.isFinal(field.getModifiers())))
                {
                    field.setAccessible(true);
                }
                final ExportMetric annotation = field.getAnnotation(ExportMetric.class);
                final String gaugeName = MetricRegistry.name(obj.getClass(), annotation.name());

                registry.register(gaugeName, (Gauge<Object>) () -> {
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

                logger.debug("Registered new metric for field " + field.getName() + " in class " + obj.getClass());
            }
        }

        for (Method method : obj.getClass().getDeclaredMethods())
        {
            if (method.isAnnotationPresent(ExportMetric.class))
            {
                //methods declared as metrics must not accept parameters.
                if (method.getParameterCount() > 0)
                {
                    continue;
                }

                final ExportMetric annotation = method.getAnnotation(ExportMetric.class);
                final String gaugeName = MetricRegistry.name(obj.getClass(), annotation.name());

                registry.register(gaugeName, (Gauge<Object>) () -> {

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

                logger.debug("Registered new metric for method " + method.getName() + " in class " + obj.getClass());
            }
        }
    }
}
