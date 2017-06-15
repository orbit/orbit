/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.interceptable;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.ActorClassFinder;
import cloud.orbit.actors.extensions.interceptor.FinderInterceptor;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class InterceptableActorClassFinder extends InterceptableActorExtension<FinderInterceptor, ActorClassFinder>
        implements ActorClassFinder
{
    public InterceptableActorClassFinder(final ActorClassFinder extension,
                                         final List<FinderInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableActorClassFinder(final ActorClassFinder extension)
    {
        super(extension);
    }

    @Override
    public <T extends Actor> Class<? extends T> findActorImplementation(final Class<T> actorInterface)
    {
        if (!hasInterceptors())
        {
            return extension.findActorImplementation(actorInterface);
        }
        InterceptedValue<Class<T>> interceptedActorInterface = InterceptedValue.of(actorInterface);
        intercept(interceptor -> interceptor.preFindActorImplementation(interceptedActorInterface));
        InterceptedValue<Class<? extends T>> interceptedActorImplementation =
                InterceptedValue.of(extension.findActorImplementation(interceptedActorInterface.get()));
        intercept(interceptor ->
                interceptor.postFindActorImplementation(interceptedActorInterface, interceptedActorImplementation));
        return interceptedActorImplementation.get();
    }

    @Override
    public <T extends Actor> Collection<Class<? extends T>> findActorInterfaces(final Predicate<Class<T>> predicate)
    {
        if (!hasInterceptors())
        {
            return extension.findActorInterfaces(predicate);
        }
        InterceptedValue<Predicate<Class<T>>> interceptedPredicate = InterceptedValue.of(predicate);
        intercept(interceptor -> interceptor.preFindActorInterfaces(interceptedPredicate));
        InterceptedValue<Collection<Class<? extends T>>> interceptedReturnValue =
                InterceptedValue.of(extension.findActorInterfaces(interceptedPredicate.get()));
        intercept(interceptor -> interceptor.postFindActorInterfaces(interceptedPredicate, interceptedReturnValue));
        return interceptedReturnValue.get();
    }
}
