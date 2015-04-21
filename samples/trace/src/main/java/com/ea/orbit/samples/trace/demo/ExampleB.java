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

package com.ea.orbit.samples.trace.demo;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.actors.runtime.Registration;
import com.ea.orbit.concurrent.Task;

import java.util.concurrent.TimeUnit;

public class ExampleB extends OrbitActor implements IExampleB
{

    Registration timer;

    @Override
    public Task activateAsync()
    {
        int interval = 100 + ((int) Math.random() * 3000);
        timer = registerTimer(() -> callRandomA(), interval, interval, TimeUnit.MILLISECONDS);
        return super.activateAsync();
    }

    @Override
    public Task<Void> callRandomA()
    {
        if (Math.random() > 0.5d) return Task.done(); //some variance to the calls
        String id = Integer.toString((int) (Math.random() * 10));
        IExampleA a = IActor.getReference(IExampleA.class, id);
        a.someWork().join();
        return Task.done();
    }

    public Task<Integer> someWork()
    {
        return Task.fromValue(24);
    }
}
