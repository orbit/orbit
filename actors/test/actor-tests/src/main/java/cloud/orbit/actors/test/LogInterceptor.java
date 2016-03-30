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

import org.slf4j.Logger;
import org.slf4j.Marker;

public class LogInterceptor implements Logger
{
    private volatile Logger delegate;

    public LogInterceptor(final Logger delegate)
    {
        this.delegate = delegate;
    }

    protected Logger delegate()
    {
        return delegate;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    public boolean isTraceEnabled()
    {
        return delegate().isTraceEnabled();
    }

    public void trace(String msg)
    {
        delegate().trace(msg);
        if (isTraceEnabled()) message("trace", msg);
    }

    public void trace(String format, Object arg)
    {
        delegate().trace(format, arg);
        if (isTraceEnabled()) message("trace", format, arg);
    }

    public void trace(String format, Object arg1, Object arg2)
    {
        delegate().trace(format, arg1, arg2);
        if (isTraceEnabled()) message("trace", format, arg1, arg2);
    }

    public void trace(String format, Object... arguments)
    {
        delegate().trace(format, arguments);
        if (isTraceEnabled()) message("trace", format, arguments);
    }


    public void trace(String msg, Throwable t)
    {
        delegate().trace(msg, t);
        if (isTraceEnabled()) message("trace", msg, t);
    }

    public boolean isTraceEnabled(Marker marker)
    {
        return delegate().isTraceEnabled(marker);
    }

    public void trace(Marker marker, String msg)
    {
        delegate().trace(marker, msg);
        if (isTraceEnabled()) message("trace", marker, msg);
    }

    public void trace(Marker marker, String format, Object arg)
    {
        delegate().trace(marker, format, arg);
        if (isTraceEnabled()) message("trace", marker, format, arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().trace(marker, format, arg1, arg2);
        if (isTraceEnabled()) message("trace", marker, format, arg1, arg2);
    }

    public void trace(Marker marker, String format, Object... arguments)
    {
        delegate().trace(marker, format, arguments);
        if (isTraceEnabled()) message("trace", marker, format, arguments);
    }

    public void trace(Marker marker, String msg, Throwable t)
    {
        delegate().trace(marker, msg, t);
        if (isTraceEnabled()) message("trace", marker, msg, t);
    }

    public boolean isDebugEnabled()
    {
        return delegate().isDebugEnabled();
    }

    public void debug(String msg)
    {
        delegate().debug(msg);
        if (isDebugEnabled()) message("debug", msg);
    }

    public void debug(String format, Object arg)
    {
        delegate().debug(format, arg);
        if (isDebugEnabled()) message("debug", format, arg);
    }

    public void debug(String format, Object arg1, Object arg2)
    {
        delegate().debug(format, arg1, arg2);
        if (isDebugEnabled()) message("debug", format, arg1, arg2);
    }

    public void debug(String format, Object... arguments)
    {
        delegate().debug(format, arguments);
        if (isDebugEnabled()) message("debug", format, arguments);
    }

    public void debug(String msg, Throwable t)
    {
        delegate().debug(msg, t);
        if (isDebugEnabled()) message("debug", msg, t);
    }

    public boolean isDebugEnabled(Marker marker)
    {
        return delegate().isDebugEnabled(marker);
    }

    public void debug(Marker marker, String msg)
    {
        delegate().debug(marker, msg);
        if (isDebugEnabled()) message("debug", marker, msg);
    }

    public void debug(Marker marker, String format, Object arg)
    {
        delegate().debug(marker, format, arg);
        if (isDebugEnabled()) message("debug", marker, format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().debug(marker, format, arg1, arg2);
        if (isDebugEnabled()) message("debug", marker, format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object... arguments)
    {
        delegate().debug(marker, format, arguments);
        if (isDebugEnabled()) message("debug", marker, format, arguments);
    }

    public void debug(Marker marker, String msg, Throwable t)
    {
        delegate().debug(marker, msg, t);
        if (isDebugEnabled()) message("debug", marker, msg, t);
    }

    public boolean isInfoEnabled()
    {
        return delegate().isInfoEnabled();
    }

    public void info(String msg)
    {
        delegate().info(msg);
        if (isInfoEnabled()) message("info", msg);
    }

    public void info(String format, Object arg)
    {
        delegate().info(format, arg);
        if (isInfoEnabled()) message("info", format, arg);
    }

    public void info(String format, Object arg1, Object arg2)
    {
        delegate().info(format, arg1, arg2);
        if (isInfoEnabled()) message("info", format, arg1, arg2);
    }

    public void info(String format, Object... arguments)
    {
        delegate().info(format, arguments);
        if (isInfoEnabled()) message("info", format, arguments);
    }

    public void info(String msg, Throwable t)
    {
        delegate().info(msg, t);
        if (isInfoEnabled()) message("info", msg, t);
    }

    public boolean isInfoEnabled(Marker marker)
    {
        return delegate().isInfoEnabled(marker);
    }

    public void info(Marker marker, String msg)
    {
        delegate().info(marker, msg);
        if (isInfoEnabled()) message("info", marker, msg);
    }

    public void info(Marker marker, String format, Object arg)
    {
        delegate().info(marker, format, arg);
        if (isInfoEnabled()) message("info", marker, format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().info(marker, format, arg1, arg2);
        if (isInfoEnabled()) message("info", marker, format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object... arguments)
    {
        delegate().info(marker, format, arguments);
        if (isInfoEnabled()) message("info", marker, format, arguments);
    }

    public void info(Marker marker, String msg, Throwable t)
    {
        delegate().info(marker, msg, t);
        if (isInfoEnabled()) message("info", marker, msg, t);
    }

    public boolean isWarnEnabled()
    {
        return delegate().isWarnEnabled();
    }

    public void warn(String msg)
    {
        delegate().warn(msg);
        if (isWarnEnabled()) message("warn", msg);
    }

    public void warn(String format, Object arg)
    {
        delegate().warn(format, arg);
        if (isWarnEnabled()) message("warn", format, arg);
    }

    public void warn(String format, Object arg1, Object arg2)
    {
        delegate().warn(format, arg1, arg2);
        if (isWarnEnabled()) message("warn", format, arg1, arg2);
    }

    public void warn(String format, Object... arguments)
    {
        delegate().warn(format, arguments);
        if (isWarnEnabled()) message("warn", format, arguments);
    }

    public void warn(String msg, Throwable t)
    {
        delegate().warn(msg, t);
        if (isWarnEnabled()) message("warn", msg, t);
    }

    public boolean isWarnEnabled(Marker marker)
    {
        return delegate().isWarnEnabled(marker);
    }

    public void warn(Marker marker, String msg)
    {
        delegate().warn(marker, msg);
        if (isWarnEnabled()) message("warn", marker, msg);
    }

    public void warn(Marker marker, String format, Object arg)
    {
        delegate().warn(marker, format, arg);
        if (isWarnEnabled()) message("warn", marker, format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().warn(marker, format, arg1, arg2);
        if (isWarnEnabled()) message("warn", marker, format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object... arguments)
    {
        delegate().warn(marker, format, arguments);
        if (isWarnEnabled()) message("warn", marker, format, arguments);
    }

    public void warn(Marker marker, String msg, Throwable t)
    {
        delegate().warn(marker, msg, t);
        if (isWarnEnabled()) message("warn", marker, msg, t);
    }

    public boolean isErrorEnabled()
    {
        return delegate().isErrorEnabled();
    }

    public void error(String msg)
    {
        delegate().error(msg);
        if (isErrorEnabled()) message("error", msg);
    }

    public void error(String format, Object arg)
    {
        delegate().error(format, arg);
        if (isErrorEnabled()) message("error", format, arg);
    }

    public void error(String format, Object arg1, Object arg2)
    {
        delegate().error(format, arg1, arg2);
        if (isErrorEnabled()) message("error", format, arg1, arg2);
    }

    public void error(String format, Object... arguments)
    {
        delegate().error(format, arguments);
        if (isErrorEnabled()) message("error", format, arguments);
    }

    public void error(String msg, Throwable t)
    {
        delegate().error(msg, t);
        if (isErrorEnabled()) message("error", msg, t);
    }

    public boolean isErrorEnabled(Marker marker)
    {
        return delegate().isErrorEnabled(marker);
    }

    public void error(Marker marker, String msg)
    {
        delegate().error(marker, msg);
        if (isErrorEnabled()) message("error", marker, msg);
    }

    public void error(Marker marker, String format, Object arg)
    {
        delegate().error(marker, format, arg);
        if (isErrorEnabled()) message("error", marker, format, arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().error(marker, format, arg1, arg2);
        if (isErrorEnabled()) message("error", marker, format, arg1, arg2);
    }

    public void error(Marker marker, String format, Object... arguments)
    {
        delegate().error(marker, format, arguments);
        if (isErrorEnabled()) message("error", marker, format, arguments);
    }


    public void error(Marker marker, String msg, Throwable t)
    {
        delegate().error(marker, msg, t);
        if (isErrorEnabled()) message("error", marker, msg, t);
    }

    protected void message(final String type, final Marker marker, final String format, final Object... arguments)
    {

    }

    protected void message(final String type, final Marker marker, final String msg, final Throwable t)
    {

    }

    protected void message(final String type, final String format, final Object... arguments)
    {
        message(type, null, format, arguments);
    }

    protected void message(final String type, final String msg, final Throwable t)
    {
        message(type, null, msg, t);
    }

}
