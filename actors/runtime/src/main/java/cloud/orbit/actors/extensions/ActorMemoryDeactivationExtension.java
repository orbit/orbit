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

package cloud.orbit.actors.extensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.runtime.ActorBaseEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * Created by joeh on 2017-05-15.
 */
public class ActorMemoryDeactivationExtension implements ActorDeactivationExtension
{
    private static final Logger logger = LoggerFactory.getLogger(ActorMemoryDeactivationExtension.class);

    private final int maxMemoryPct;
    private final int actorCullPct;
    private final Duration maxFrequency;

    private long lastCulling = 0;

    public ActorMemoryDeactivationExtension(final int maxMemoryPct, final int actorCullPct, final Duration maxFrequency)
    {
        this.maxMemoryPct = maxMemoryPct;
        this.actorCullPct = actorCullPct;
        this.maxFrequency = maxFrequency;
    }

    @Override
    public void cleanupActors(final Collection<ActorBaseEntry<?>> actorEntries, final Set<ActorBaseEntry<?>> toRemove)
    {
        final long currentTime = System.currentTimeMillis();
        final Runtime runtime = Runtime.getRuntime();
        final float maxMem = runtime.maxMemory() / 1024 / 1024;
        final float freeMem = runtime.freeMemory() / 1024 / 1024;
        final int memoryPct  = (int) (((maxMem - freeMem) / maxMem) * 100.0f);

        if(lastCulling + maxFrequency.toMillis() < currentTime)
        {
            if(memoryPct > maxMemoryPct) {
                final int actorCount = actorEntries.size();
                final int cullCount = actorCount * (actorCullPct / 100);

                if(logger.isWarnEnabled())
                {
                    logger.warn("JVM is reporting {}% memory usage. Max memory use is set at {}% with a cull setting of {}%. Attemping to deactivate {} of {} actors in accordance with cull options."
                    , memoryPct, maxMemoryPct, actorCullPct, cullCount, actorCount);
                }

                actorEntries.stream()
                        .sorted((Comparator.comparingLong(ActorBaseEntry::getLastAccess)))
                        .limit(cullCount)
                        .forEach(toRemove::add);

                lastCulling = System.currentTimeMillis();
            }
        }
    }
}
