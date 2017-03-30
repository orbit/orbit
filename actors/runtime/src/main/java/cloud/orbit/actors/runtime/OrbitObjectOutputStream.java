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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.ActorObserver;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class OrbitObjectOutputStream extends ObjectOutputStream
{

    private final BasicRuntime runtime;

    public OrbitObjectOutputStream(final OutputStream outputStream, final BasicRuntime runtime) throws IOException
    {
        super(outputStream);
        this.runtime = runtime;
        enableReplaceObject(true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object replaceObject(final Object obj) throws IOException
    {
        final RemoteReference reference;
        if (!(obj instanceof RemoteReference))
        {
            if (obj instanceof AbstractActor)
            {
                reference = ((AbstractActor) obj).reference;
            }
            else if (obj instanceof ActorObserver)
            {
                ActorObserver objectReference = runtime.registerObserver(null, (ActorObserver) obj);
                reference = (RemoteReference) objectReference;
            }
            else
            {
                return super.replaceObject(obj);
            }
        }
        else
        {
            reference = (RemoteReference) obj;
        }
        final ReferenceReplacement replacement = new ReferenceReplacement();
        replacement.address = reference.address;
        replacement.interfaceClass = reference._interfaceClass();
        replacement.id = reference.id;
        return replacement;
    }
}
