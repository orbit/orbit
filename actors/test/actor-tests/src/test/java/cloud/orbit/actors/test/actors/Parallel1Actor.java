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

package cloud.orbit.actors.test.actors;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Parallel1Actor extends AbstractActor implements Parallel1
{
    private static final int NUM_ELEMENTS = 10_000_000;

    private final List<Integer> list = IntStream.range(0, NUM_ELEMENTS).boxed().collect(Collectors.toList());

    @Override
    public Task<Void> read() {
        this.list.forEach(i -> {
        });
        return Task.done();
    }

    @Override
    public Task<Void> writeAttached() {
        return this.otherActor().nothing().thenCompose(this::write);
    }

    @Override
    public Task<Void> writeDetached() {
        this.otherActor().nothing().thenCompose(this::write);
        return Task.done();
    }

    private Parallel2 otherActor() {
        return Actor.getReference(Parallel2.class, this.getIdentity());
    }

    private Task<Void> write() {
        IntStream.range(0, NUM_ELEMENTS).forEach(i -> {
            if (i % 10000 == 0) {
                this.list.remove(i);
            }
        });
        return Task.done();
    }
}
