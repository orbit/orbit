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

package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.extensions.DefaultLoggerExtension;
import com.ea.orbit.actors.extensions.LoggerExtension;
import com.ea.orbit.actors.net.Handler;
import com.ea.orbit.actors.peer.PeerExtension;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.concurrent.ConcurrentHashSet;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;


public class TestLogger implements LoggerExtension, PeerExtension
{

    private String nodeId = "";
    private ActorBaseTest actorBaseTest;
    private DefaultLoggerExtension defaultLogger = new DefaultLoggerExtension();
    ConcurrentHashSet<Object> classesToDebug = new ConcurrentHashSet<>();

    public TestLogger(final ActorBaseTest actorBaseTest)
    {
        this.actorBaseTest = actorBaseTest;
    }

    public TestLogger(final TestLogger parentLogger, final String nodeId)
    {
        this.actorBaseTest = parentLogger.actorBaseTest;
        this.classesToDebug = parentLogger.classesToDebug;
        this.nodeId = nodeId;
    }

    @Override
    public Logger getLogger(final Object object)
    {
        final Logger logger = defaultLogger.getLogger(object);

        final Class targetClass = object == null ? null
                : object instanceof String ? null
                : object instanceof Class ? (Class) object
                : object.getClass();

        String target;
        if (object instanceof Actor)
        {
            final RemoteReference reference = RemoteReference.from((Actor) object);
            target = (RemoteReference.getInterfaceClass(reference).getSimpleName()
                    + ":" + RemoteReference.getId(reference)).replaceAll("[\"\\t\\r\\n]", "");
        }
        else if (object instanceof Handler)
        {
            target = object.getClass().getSimpleName() + ":" + object.hashCode();
        }
        else
        {
            target = logger.getName();
        }
        return new LogInterceptor(logger)
        {
            @Override
            public boolean isErrorEnabled()
            {
                if (targetClass != null && classesToDebug.contains(targetClass))
                {
                    return true;
                }
                return super.isErrorEnabled();
            }

            @Override
            public boolean isDebugEnabled()
            {
                if (targetClass != null && classesToDebug.contains(targetClass))
                {
                    return true;
                }
                return super.isDebugEnabled();
            }

            @Override
            public boolean isWarnEnabled()
            {
                if (targetClass != null && classesToDebug.contains(targetClass))
                {
                    return true;
                }
                return super.isWarnEnabled();
            }

            @Override
            public boolean isInfoEnabled()
            {
                if (targetClass != null && classesToDebug.contains(targetClass))
                {
                    return true;
                }
                return super.isInfoEnabled();
            }

            @Override
            protected void message(final String type, final Marker marker, final String format, final Object... arguments)
            {
                String fmtMessage = format;
                if (arguments != null && arguments.length > 0)
                {
                    try
                    {
                        fmtMessage = MessageFormatter.format(format, arguments).getMessage();
                    }
                    catch (Exception ex)
                    {
                        logger.error("Error formatting message: {}", format);
                    }
                }
                final String message = (!"info".equalsIgnoreCase(type) ? type + ": " : "") + fmtMessage;
                String position = "over";
                note(position, target, message);
            }
        };
    }

    private void note(final String position, final String target, final String message)
    {
        final StringBuilder note = new StringBuilder("note ");
        note.append(position).append(" \"").append(target).append('"');
        if (message.contains("\n"))
        {
            note.append("\r\n");
            note.append(message);
            note.append("\r\n").append("end note");

        }
        else
        {
            note.append(": ").append(message);
        }
        actorBaseTest.sequenceDiagram.add(note.toString());
    }

    public void enableDebugFor(Class clazz)
    {
        classesToDebug.add(clazz);
    }

    public void disableDebugFor(Class clazz)
    {
        classesToDebug.add(clazz);
    }

    public void disableDebugForAll()
    {
        classesToDebug.clear();
    }
}
