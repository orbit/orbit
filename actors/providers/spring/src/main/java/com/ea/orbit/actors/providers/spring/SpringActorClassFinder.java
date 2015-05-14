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

package com.ea.orbit.actors.providers.spring;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.providers.IActorClassFinder;
import com.ea.orbit.actors.runtime.ReminderController;
import com.ea.orbit.actors.runtime.ReminderControllerActor;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link SpringActorClassFinder} uses Spring utility classes to locate actor implementations.
 *
 * <code>orbit.actors.basePackage</code> must be set to the base package which will be scanned for actor interfaces / implementations.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
@Component
public class SpringActorClassFinder implements IActorClassFinder
{

    @Autowired
    private ActorInterfaceProvider actorInterfaceProvider;

    @Autowired
    private ActorImplementationProvider actorImplementationProvider;

    @Value("${orbit.actors.basePackage}")
    private String basePackage;

    private Map<Class<?>, Class<?>> concreteImplementations = new HashMap<>();

    @PostConstruct
    public void initialize()
    {
        if (!StringUtils.hasText(basePackage))
        {
            throw new NullPointerException("orbit.actors.basePackage must be set!");
        }
        Map<Class<?>, Class<?>> implementations = actorImplementationProvider.getActorImplementations(actorInterfaceProvider.getActorInterfaces());
        implementations.put(ReminderController.class, ReminderControllerActor.class); // built in Orbit actor
        this.concreteImplementations = implementations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Actor> Class<? extends T> findActorImplementation(Class<T> clazz)
    {
        return (Class<T>) concreteImplementations.get(clazz);
    }

    @Component
    private class ActorImplementationProvider extends ClassPathScanningCandidateComponentProvider
    {

        @Autowired
        public ActorImplementationProvider()
        {
            super(false);
            addIncludeFilter(new AssignableTypeFilter(Actor.class));
        }

        public Map<Class<?>, Class<?>> getActorImplementations(List<Class<?>> clazzInterfaces)
        {
            Map<Class<?>, Class<?>> actorImplementations = new HashMap<>();
            for (BeanDefinition candidate : findCandidateComponents(basePackage))
            {
                Class<?> implementation = ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                for (Class<?> clazzInterface : clazzInterfaces)
                {
                    if (clazzInterface.isAssignableFrom(implementation))
                    {
                        Class<?> old = actorImplementations.put(clazzInterface, implementation);
                        if (old != null)
                        {
                            throw new IllegalStateException("Multiple actor implementations found for " + clazzInterface);
                        }
                        break;
                    }
                }
            }
            return actorImplementations;
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition)
        {
            return beanDefinition.getMetadata().isConcrete();
        }
    }

    @Component
    private class ActorInterfaceProvider extends ClassPathScanningCandidateComponentProvider
    {

        @Autowired
        public ActorInterfaceProvider()
        {
            super(false);
            addIncludeFilter(new AssignableTypeFilter(Actor.class));
        }

        public List<Class<?>> getActorInterfaces()
        {
            return findCandidateComponents(basePackage).stream().map(candidate -> (ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader())))
                    .collect(Collectors.toList());
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition)
        {
            return beanDefinition.getMetadata().isInterface();
        }
    }
}
