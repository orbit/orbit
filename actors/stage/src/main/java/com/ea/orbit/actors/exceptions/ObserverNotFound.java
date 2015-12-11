package com.ea.orbit.actors.exceptions;


import com.ea.orbit.exception.UncheckedException;

public class ObserverNotFound extends UncheckedException
{
    public ObserverNotFound()
    {
    }

    public ObserverNotFound(final Throwable cause)
    {
        super(cause);
    }

    public ObserverNotFound(final String message)
    {
        super(message);
    }

    public ObserverNotFound(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public ObserverNotFound(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
