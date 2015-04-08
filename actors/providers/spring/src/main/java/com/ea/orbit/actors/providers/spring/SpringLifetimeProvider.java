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

import com.ea.orbit.actors.providers.ILifetimeProvider;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * {@link SpringLifetimeProvider} allows Spring DI for {@link com.ea.orbit.actors.runtime.OrbitActor}.
 * Autowire mode can be set via <code>orbit.actors.autowireMode</code>.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
@Component
public class SpringLifetimeProvider implements ILifetimeProvider {

    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Value("${orbit.actors.autowireMode:" + AutowireCapableBeanFactory.AUTOWIRE_BY_NAME + "}")
    private int autowireMode;

    @Override
    public Task preActivation(OrbitActor orbitActor) {
        autowireCapableBeanFactory.autowireBeanProperties(orbitActor, autowireMode, false);
        return Task.done();
    }
}
