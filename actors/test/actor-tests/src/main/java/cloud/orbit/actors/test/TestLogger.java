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

package cloud.orbit.actors.test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.DefaultLoggerExtension;
import cloud.orbit.actors.extensions.LoggerExtension;
import cloud.orbit.actors.net.Handler;
import cloud.orbit.actors.peer.PeerExtension;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.ConcurrentHashSet;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestLogger implements LoggerExtension, PeerExtension
{

    private String nodeId = "";
    private final DefaultLoggerExtension defaultLogger = new DefaultLoggerExtension();
    private final ConcurrentHashSet<Object> classesToDebug;
    private final StringBuilder logText;
    protected final List<String> sequenceDiagram;
    private boolean visible;

    public TestLogger()
    {
        classesToDebug = new ConcurrentHashSet<>();
        logText = new StringBuilder();
        sequenceDiagram = Collections.synchronizedList(new ArrayList<>());
    }

    public TestLogger(final TestLogger parentLogger, final String nodeId)
    {
        this.classesToDebug = parentLogger.classesToDebug;
        this.sequenceDiagram = parentLogger.sequenceDiagram;
        this.logText = parentLogger.logText;
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
                        fmtMessage = "Error formatting message: " + format;
                    }
                }
                final String message = (!"info".equalsIgnoreCase(type) ? type + ": " : "") + fmtMessage;
                write(type + " " + new Date() + " " + target + " " + message);
                String position = "over";
                note(position, target, message);
            }
        };
    }


    public synchronized void write(final CharSequence buffer)
    {
        // truncating the log
        if (logText.length() > 250e6)
        {
            logText.setLength(64000);
            logText.append("The log was truncated!").append("\r\n");
        }
        logText.append(buffer).append("\r\n");
        if (visible)
        {
            System.out.println(buffer);
        }
    }


    private void note(final String position, final String target, final String message)
    {
        final StringBuilder note = new StringBuilder("note ");
        note.append(position).append(" \"").append(target).append('"');
        if (message.contains("\n"))
        {
            note.append("\r\n");
            note.append(wrap(message, 40, "\r\n", true));
            note.append("\r\n").append("end note");

        }
        else
        {
            note.append(": ").append(message);
        }
        sequenceDiagram.add(note.toString());
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

    static String wrap(final String str, final int width, final String lineBreak, final boolean breakWords)
    {
        final int length = str.length();
        if (length < width)
        {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int pos = 0;
        for (; length - start > width; )
        {
            int possibleBreak = -1;
            for (; pos - start < width && pos < length; pos++)
            {
                char ch = str.charAt(pos);
                if (!Character.isDigit(ch) && !Character.isAlphabetic(ch))
                {
                    possibleBreak = pos;
                }
            }
            if (possibleBreak > 0)
            {
                sb.append(str, start, possibleBreak + 1);
                start = possibleBreak + 1;
            }
            else
            {
                sb.append(str, start, pos);
                start = pos;
            }
            if (start < length)
            {
                sb.append(lineBreak);
            }
        }
        if (start < length)
        {
            sb.append(str, start, length);
        }
        return sb.toString();
    }

    public void clear()
    {
        sequenceDiagram.clear();
    }

    public CharSequence getLogText()
    {
        return logText;
    }

    public void addToSequenceDiagram(final String sequenceEntry)
    {
        sequenceDiagram.add(sequenceEntry);
    }

    public void dumpMessages(final String fileName)
    {
        final PrintStream out = System.out;
        if (sequenceDiagram.size() > 0)
        {
            final Path seqUml = Paths.get(fileName);
            try
            {
                Files.createDirectories(seqUml.getParent());

                Files.write(seqUml,
                        Stream.concat(Stream.concat(
                                Stream.of("@startuml"),
                                Stream.of(sequenceDiagram.toArray()).map(o -> (String) o)),
                                Stream.of("@enduml")
                        ).collect(Collectors.toList()));
                out.println(seqUml.toUri());
            }
            catch (Exception ex)
            {
                new IOException("error dumping messages: " + ex.getMessage(), ex).printStackTrace();
            }
        }
        else
        {
            out.println("No messages to dump!");
        }
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(final boolean visible)
    {
        this.visible = visible;
    }
}
