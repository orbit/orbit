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

import cloud.orbit.actors.concurrent.MultiExecutionSerializer;
import cloud.orbit.actors.extensions.LoggerExtension;
import cloud.orbit.actors.extensions.StorageExtension;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskFunction;
import cloud.orbit.exception.NotImplementedException;

import org.slf4j.Logger;

public abstract class ActorBaseEntry<T extends AbstractActor> implements LocalObjects.LocalObjectEntry<T>
{
    final RemoteReference<T> reference;
    Class<T> concreteClass;
    protected ActorRuntime runtime;
    MultiExecutionSerializer<Object> executionSerializer;
    LoggerExtension loggerExtension;
    StorageExtension storageExtension;
    private boolean deactivated;
    private Logger logger;
    protected long lastAccess;

    public ActorBaseEntry(final RemoteReference reference)
    {
        this.reference = reference;
    }

    @Override
    public RemoteReference<T> getRemoteReference()
    {
        return reference;
    }

    @Override
    public <R> Task<R> run(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {
        throw new NotImplementedException();
    }

    protected Logger getLogger()
    {
        return logger != null ? logger : (logger = loggerExtension.getLogger(reference._interfaceClass()));
    }

    public void setConcreteClass(final Class<T> concreteClass)
    {
        this.concreteClass = concreteClass;
    }

    public void setRuntime(final ActorRuntime runtime)
    {
        this.runtime = runtime;
        this.lastAccess = runtime.clock().millis();
    }

    public void setExecutionSerializer(final MultiExecutionSerializer<Object> executionSerializer)
    {
        this.executionSerializer = executionSerializer;
    }

    public void setStorageExtension(final StorageExtension storageExtension)
    {
        this.storageExtension = storageExtension;
    }

    public void setLoggerExtension(final LoggerExtension loggerExtension)
    {
        this.loggerExtension = loggerExtension;
    }

    public boolean isDeactivated()
    {
        return deactivated;
    }

    /**
     * This must not fail. If errors it should log them instead of throwing
     */

    public abstract Task<Void> deactivate();


    public void setDeactivated(final boolean deactivated)
    {
        this.deactivated = deactivated;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }
}
