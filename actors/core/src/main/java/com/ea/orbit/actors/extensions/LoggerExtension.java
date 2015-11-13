package com.ea.orbit.actors.extensions;

import org.slf4j.Logger;

public interface LoggerExtension extends ActorExtension
{
    Logger getLogger(Object object);
}
