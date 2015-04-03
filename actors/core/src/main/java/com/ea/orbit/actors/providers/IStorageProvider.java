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

package com.ea.orbit.actors.providers;


import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.concurrent.Task;

/**
 * Storage providers are used by the orbit actors framework to load and store actor states.
 */
public interface IStorageProvider extends IOrbitProvider
{
    /**
     * Asynchronously clears an actors state.
     * @param reference an reference to the actor (contains the interface name and actor key)
     * @param state the state object, not modified.
     * @return a completion promise
     */
    Task<Void> clearState(ActorReference<?> reference, Object state);

    /**
     * Asynchronously reads an actors state.
     * @param reference an reference to the actor (contains the interface name and actor key)
     * @param state the state object, modified by the call
     * @return a completion promise
     */
    Task<Boolean> readState(ActorReference<?> reference, Object state);

    /**
     * Asynchronously writes an actors state.
     * @param reference an reference to the actor (contains the interface name and actor key)
     * @param state the state object, not modified by the call
     * @return a completion promise
     */
    Task<Void> writeState(ActorReference<?> reference, Object state);
}
