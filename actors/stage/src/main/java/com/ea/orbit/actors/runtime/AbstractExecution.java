package com.ea.orbit.actors.runtime;


import com.ea.orbit.actors.concurrent.MultiExecutionSerializer;
import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

public abstract class AbstractExecution extends HandlerAdapter
{
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected MultiExecutionSerializer<Object> executionSerializer;
    protected int maxQueueSize = 10000;

    protected final LongAdder messagesReceived = new LongAdder();
    protected final LongAdder messagesHandled = new LongAdder();
    protected final LongAdder refusedExecutions = new LongAdder();

    public abstract Task<Void> cleanup();

    public void setExecutionSerializer(final MultiExecutionSerializer<Object> executionSerializer)
    {
        this.executionSerializer = executionSerializer;
    }
}
