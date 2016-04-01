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

package cloud.orbit.actors.cloner;

import cloud.orbit.actors.annotation.Immutable;
import cloud.orbit.actors.runtime.Message;
import cloud.orbit.actors.runtime.MessageDefinitions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {$link CloneHelper} provides basic checks for whether an object requires cloning (is mutable).
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class CloneHelper
{
    private static final Set<Class> IMMUTABLES = new HashSet<>(Arrays.asList(
            byte.class,
            char.class,
            boolean.class,
            int.class,
            long.class,
            double.class,
            float.class,
            java.lang.String.class,
            java.lang.Integer.class,
            java.lang.Byte.class,
            java.lang.Character.class,
            java.lang.Short.class,
            java.lang.Boolean.class,
            java.lang.Long.class,
            java.lang.Double.class,
            java.lang.Float.class,
            java.lang.StackTraceElement.class,
            java.math.BigInteger.class,
            java.math.BigDecimal.class,
            java.io.File.class,
            java.util.Locale.class,
            java.util.UUID.class,
            java.util.Collections.class,
            java.net.URL.class,
            java.net.URI.class,
            java.net.Inet4Address.class,
            java.net.Inet6Address.class,
            java.net.InetSocketAddress.class
    ));

    public static boolean needsCloning(Object object)
    {
        if (object == null)
        {
            return false;
        }
        if (object instanceof Message)
        {
            return needsCloning((Message) object);
        }
        return !isObjectConsideredImmutable(object);
    }

    private static boolean needsCloning(Message message)
    {
        // this can be improved by looking into the
        // argument/return types of ClassId.MethodId and caching the decision.

        final Object payload = message.getPayload();
        if (payload == null)
        {
            return false;
        }
        switch (message.getMessageType())
        {
            case MessageDefinitions.ONE_WAY_MESSAGE:
            case MessageDefinitions.REQUEST_MESSAGE:
                if (payload instanceof Object[])
                {
                    Object[] arr = (Object[]) payload;
                    for (int i = arr.length; --i >= 0; )
                    {
                        Object obj = arr[i];
                        if (!isObjectConsideredImmutable(obj))
                        {
                            return true;
                        }
                    }
                    // no cloning required, true also for arr.length = 0
                    return false;
                }
                break;
            case MessageDefinitions.RESPONSE_OK:
                // already test for null in the beginning of the method.
                if (isObjectConsideredImmutable(payload))
                {
                    return false;
                }
                break;
        }
        return true;
    }

    private static boolean isObjectConsideredImmutable(Object object)
    {
        if (object == null)
        {
            return true;
        }
        if (object.getClass().isAnnotationPresent(Immutable.class))
        {
            return true;
        }
        if (IMMUTABLES.contains(object.getClass()))
        {
            return true;
        }
        if (object.getClass().isEnum())
        {
            return true;
        }
        return false;
    }
}
