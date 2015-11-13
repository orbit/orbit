package com.ea.orbit.actors.test;

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
        message("trace", msg);
    }

    public void trace(String format, Object arg)
    {
        delegate().trace(format, arg);
        message("trace", format, arg);
    }

    public void trace(String format, Object arg1, Object arg2)
    {
        delegate().trace(format, arg1, arg2);
        message("trace", format, arg1, arg2);
    }

    public void trace(String format, Object... arguments)
    {
        delegate().trace(format, arguments);
        message("trace", format, arguments);
    }


    public void trace(String msg, Throwable t)
    {
        delegate().trace(msg, t);
        message("trace", msg, t);
    }

    public boolean isTraceEnabled(Marker marker)
    {
        return delegate().isTraceEnabled(marker);
    }

    public void trace(Marker marker, String msg)
    {
        delegate().trace(marker, msg);
        message("trace", marker, msg);
    }

    public void trace(Marker marker, String format, Object arg)
    {
        delegate().trace(marker, format, arg);
        message("trace", marker, format, arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().trace(marker, format, arg1, arg2);
        message("trace", marker, format, arg1, arg2);
    }

    public void trace(Marker marker, String format, Object... arguments)
    {
        delegate().trace(marker, format, arguments);
        message("trace", marker, format, arguments);
    }

    public void trace(Marker marker, String msg, Throwable t)
    {
        delegate().trace(marker, msg, t);
        message("trace", marker, msg, t);
    }

    public boolean isDebugEnabled()
    {
        return delegate().isDebugEnabled();
    }

    public void debug(String msg)
    {
        delegate().debug(msg);
        message("debug", msg);
    }

    public void debug(String format, Object arg)
    {
        delegate().debug(format, arg);
        message("debug", format, arg);
    }

    public void debug(String format, Object arg1, Object arg2)
    {
        delegate().debug(format, arg1, arg2);
        message("debug", format, arg1, arg2);
    }

    public void debug(String format, Object... arguments)
    {
        delegate().debug(format, arguments);
        message("debug", format, arguments);
    }

    public void debug(String msg, Throwable t)
    {
        delegate().debug(msg, t);
        message("debug", msg, t);
    }

    public boolean isDebugEnabled(Marker marker)
    {
        return delegate().isDebugEnabled(marker);
    }

    public void debug(Marker marker, String msg)
    {
        delegate().debug(marker, msg);
        message("debug", marker, msg);
    }

    public void debug(Marker marker, String format, Object arg)
    {
        delegate().debug(marker, format, arg);
        message("debug", marker, format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().debug(marker, format, arg1, arg2);
        message("debug", marker, format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object... arguments)
    {
        delegate().debug(marker, format, arguments);
        message("debug", marker, format, arguments);
    }

    public void debug(Marker marker, String msg, Throwable t)
    {
        delegate().debug(marker, msg, t);
        message("debug", marker, msg, t);
    }

    public boolean isInfoEnabled()
    {
        return delegate().isInfoEnabled();
    }

    public void info(String msg)
    {
        delegate().info(msg);
        message("info", msg);
    }

    public void info(String format, Object arg)
    {
        delegate().info(format, arg);
        message("info", format, arg);
    }

    public void info(String format, Object arg1, Object arg2)
    {
        delegate().info(format, arg1, arg2);
        message("info", format, arg1, arg2);
    }

    public void info(String format, Object... arguments)
    {
        delegate().info(format, arguments);
        message("info", format, arguments);
    }

    public void info(String msg, Throwable t)
    {
        delegate().info(msg, t);
        message("info", msg, t);
    }

    public boolean isInfoEnabled(Marker marker)
    {
        return delegate().isInfoEnabled(marker);
    }

    public void info(Marker marker, String msg)
    {
        delegate().info(marker, msg);
        message("info", marker, msg);
    }

    public void info(Marker marker, String format, Object arg)
    {
        delegate().info(marker, format, arg);
        message("info", marker, format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().info(marker, format, arg1, arg2);
        message("info", marker, format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object... arguments)
    {
        delegate().info(marker, format, arguments);
        message("info", marker, format, arguments);
    }

    public void info(Marker marker, String msg, Throwable t)
    {
        delegate().info(marker, msg, t);
        message("info", marker, msg, t);
    }

    public boolean isWarnEnabled()
    {
        return delegate().isWarnEnabled();
    }

    public void warn(String msg)
    {
        delegate().warn(msg);
        message("warn", msg);
    }

    public void warn(String format, Object arg)
    {
        delegate().warn(format, arg);
        message("warn", format, arg);
    }

    public void warn(String format, Object arg1, Object arg2)
    {
        delegate().warn(format, arg1, arg2);
        message("warn", format, arg1, arg2);
    }

    public void warn(String format, Object... arguments)
    {
        delegate().warn(format, arguments);
        message("warn", format, arguments);
    }

    public void warn(String msg, Throwable t)
    {
        delegate().warn(msg, t);
        message("warn", msg, t);
    }

    public boolean isWarnEnabled(Marker marker)
    {
        return delegate().isWarnEnabled(marker);
    }

    public void warn(Marker marker, String msg)
    {
        delegate().warn(marker, msg);
        message("warn", marker, msg);
    }

    public void warn(Marker marker, String format, Object arg)
    {
        delegate().warn(marker, format, arg);
        message("warn", marker, format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().warn(marker, format, arg1, arg2);
        message("warn", marker, format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object... arguments)
    {
        delegate().warn(marker, format, arguments);
        message("warn", marker, format, arguments);
    }

    public void warn(Marker marker, String msg, Throwable t)
    {
        delegate().warn(marker, msg, t);
        message("warn", marker, msg, t);
    }

    public boolean isErrorEnabled()
    {
        return delegate().isErrorEnabled();
    }

    public void error(String msg)
    {
        delegate().error(msg);
        message("error", msg);
    }

    public void error(String format, Object arg)
    {
        delegate().error(format, arg);
        message("error", format, arg);
    }

    public void error(String format, Object arg1, Object arg2)
    {
        delegate().error(format, arg1, arg2);
        message("error", format, arg1, arg2);
    }

    public void error(String format, Object... arguments)
    {
        delegate().error(format, arguments);
        message("error", format, arguments);
    }

    public void error(String msg, Throwable t)
    {
        delegate().error(msg, t);
        message("error", msg, t);
    }

    public boolean isErrorEnabled(Marker marker)
    {
        return delegate().isErrorEnabled(marker);
    }

    public void error(Marker marker, String msg)
    {
        delegate().error(marker, msg);
        message("error", marker, msg);
    }

    public void error(Marker marker, String format, Object arg)
    {
        delegate().error(marker, format, arg);
        message("error", marker, format, arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2)
    {
        delegate().error(marker, format, arg1, arg2);
        message("error", marker, format, arg1, arg2);
    }

    public void error(Marker marker, String format, Object... arguments)
    {
        delegate().error(marker, format, arguments);
        message("error", marker, format, arguments);
    }


    public void error(Marker marker, String msg, Throwable t)
    {
        delegate().error(marker, msg, t);
        message("error", marker, msg, t);
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
