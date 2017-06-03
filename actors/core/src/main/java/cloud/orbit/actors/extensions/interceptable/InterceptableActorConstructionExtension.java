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

import cloud.orbit.actors.extensions.ActorConstructionExtension;
import cloud.orbit.actors.extensions.interceptor.ConstructionInterceptor;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;

import java.util.List;

public class InterceptableActorConstructionExtension
        extends InterceptableActorExtension<ConstructionInterceptor, ActorConstructionExtension>
        implements ActorConstructionExtension
{
    public InterceptableActorConstructionExtension(final ActorConstructionExtension extension,
                                                   final List<ConstructionInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableActorConstructionExtension(final ActorConstructionExtension extension)
    {
        super(extension);
    }

    @Override
    public <T> T newInstance(final Class<T> concreteClass)
    {
        if (!hasInterceptors())
        {
            return extension.newInstance(concreteClass);
        }
        InterceptedValue<Class<T>> interceptedConcreteClass = InterceptedValue.of(concreteClass);
        intercept(interceptor -> interceptor.preNewInstance(interceptedConcreteClass));
        InterceptedValue<T> interceptedResult =
                InterceptedValue.of(extension.newInstance(interceptedConcreteClass.get()));
        intercept(interceptor -> interceptor.postNewInstance(interceptedConcreteClass, interceptedResult));
        return interceptedResult.get();
    }
}
