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

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * Created by joeh on 2017-05-15.
 */
public class ActorCountDeactivationExtension implements ActorDeactivationExtension
{
    private static final Logger logger = LoggerFactory.getLogger(ActorCountDeactivationExtension.class);

    private final int maxActorCount;
    private final int targetActorCount;

    public ActorCountDeactivationExtension(final int maxActorCount, final int targetActorCount)
    {
        this.maxActorCount = maxActorCount;
        this.targetActorCount = targetActorCount;
    }

    @Override
    public void cleanupActors(final Collection<ActorBaseEntry<?>> actorEntries, final Set<ActorBaseEntry<?>> toRemove)
    {
        final int currentActorCount = actorEntries.size();

        if(currentActorCount > maxActorCount)
        {
            final int countToRemove = Math.abs(targetActorCount - currentActorCount);

            if(logger.isWarnEnabled())
            {
                logger.warn("Stage has {} actors. The max actor count is set at {} with a target of {}. Attemping to deactivate {} actors to hit desired target."
                        , currentActorCount, maxActorCount, targetActorCount, countToRemove);
            }

            actorEntries.stream()
                    .sorted((Comparator.comparingLong(ActorBaseEntry::getLastAccess)))
                    .limit(countToRemove)
                    .forEach(toRemove::add);
        }
    }
}
