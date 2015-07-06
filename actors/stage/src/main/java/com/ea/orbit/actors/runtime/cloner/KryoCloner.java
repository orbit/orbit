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

package com.ea.orbit.actors.runtime.cloner;

import com.ea.orbit.actors.Actor;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.util.*;

/**
 * Kyro based object cloning implementation.
 */
public class KryoCloner implements ExecutionObjectCloner
{
    // Kryo is not thread-safe.
    private final Kryo kryo;

    public KryoCloner()
    {
        kryo = new Kryo();

        // Adding support to clone unmodifiable collections as modifiable
        // collections.

        // It has to be noted that this might not be the expected behavior
        // in some cases.

        // Without this the unmodifiable collections cause kryo to throw an
        // exception.
        kryo.addDefaultSerializer(Collection.class, new CollectionSerializer()
        {
            final List unmodifiableList = Collections.unmodifiableList(new ArrayList());
            final Set unmodifiableSet = Collections.unmodifiableSet(new HashSet());

            @Override
            protected Collection createCopy(Kryo kryo, Collection original)
            {
                if (original.getClass() == unmodifiableList.getClass() || original.getClass() == ArrayList.class)
                {
                    return new ArrayList();
                }
                if (original.getClass() == unmodifiableSet.getClass())
                {
                    return new LinkedHashSet();
                }
                return super.createCopy(kryo, original);
            }
        });
        kryo.addDefaultSerializer(Map.class, new MapSerializer()
        {
            final Map unmodifiableMap = Collections.unmodifiableMap(new HashMap());

            @Override
            protected Map createCopy(Kryo kryo, Map original)
            {
                if (original.getClass() == unmodifiableMap.getClass())
                {
                    return new LinkedHashMap<>();
                }
                return super.createCopy(kryo, original);
            }
        });

        kryo.addDefaultSerializer(Actor.class, new ImmutableObjectSerializer<Actor>(true, true));
        kryo.addDefaultSerializer(UUID.class, new ImmutableObjectSerializer<UUID>(true, true));
    }

    @Override
    public <T> T clone(final T obj)
    {
        if (obj != null)
        {
            synchronized (kryo)
            {
                try
                {
                    return kryo.copy(obj);
                } finally
                {
                    kryo.reset();
                }
            }
        }
        return null;
    }
}
