/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.runtime;

import java.util.Map;

public interface ActorState
{
    /**
     * @return a snapshot of the actor state data, as a Map of Key-Value pairs
     */
    Map<String, Object> asMap();

    /**
     * Updates the actor state
     *
     * @param values
     * @return
     */
    void putAll(Map<String, Object> values);

    /**
     * Sets an extended property not initially defined in the actor state.
     * <p>
     * Useful for extensions that modify the actor behaviour and need to pig back on the actor storage.
     * </p>
     *
     * @param key
     * @return the extended property value or null
     */
    Object getExtendedProperty(String key);

    /**
     * Sets an extended property not initially defined in the actor state.
     *
     * @param key
     * @param value null means remove the property
     */
    void setExtendedProperty(String key, Object value);

    /**
     * Invoke state event used for event sourced states
     *
     * @param eventName the event method name (they must be unique)
     * @param args      the event arguments
     * @return the event method return type (usually Task or void)
     */
    default Object invokeEvent(String eventName, Object[] args)
    {
        throw new IllegalArgumentException("Invalid event: " + eventName);
    }
}
