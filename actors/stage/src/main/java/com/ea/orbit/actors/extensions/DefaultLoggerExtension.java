package com.ea.orbit.actors.extensions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.RemoteReference;

import org.slf4j.Logger;

public class DefaultLoggerExtension implements LoggerExtension
{
    public Logger getLogger(String name)
    {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

    @Override
    public Logger getLogger(final Object object)
    {
        if (object instanceof Actor)
        {
            return getLogger(RemoteReference.getInterfaceClass(RemoteReference.from((Actor) object)).getName());
        }
        if (object instanceof Class)
        {
            return getLogger(((Class) object).getName());
        }
        if (object instanceof String)
        {
            return getLogger((String) object);
        }
        if (object != null)
        {
            return getLogger(object.getClass());
        }
        return getLogger((String) null);
    }
}
