package com.ea.orbit.actors.runtime;


import com.ea.orbit.actors.net.HandlerAdapter;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

public abstract class AbstractExecution extends HandlerAdapter
{
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected ExecutionSerializer<Object> executionSerializer;
    protected int maxQueueSize = 10000;

    protected final LongAdder messagesReceived = new LongAdder();
    protected final LongAdder messagesHandled = new LongAdder();
    protected final LongAdder refusedExecutions = new LongAdder();

    public Task<?> onMessageReceived(Message message)
    {
        int interfaceId = message.getInterfaceId();
        int methodId = message.getMethodId();
        Object key = message.getObjectId();

        EntryKey entryKey = new EntryKey(interfaceId, key);
        if (logger.isDebugEnabled())
        {
            logger.debug("onMessageReceived for: " + entryKey);
        }
        Task completion = new Task();
        if (!executionSerializer.offerJob(entryKey, () ->
                (handleOnMessageReceived(completion,
                        entryKey,
                        message)), maxQueueSize))
        {
            refusedExecutions.increment();

            if (logger.isErrorEnabled())
            {
                logger.error("Execution refused: " + key + ":" + interfaceId + ":" + methodId + ":" + message.getMessageId());
            }
            completion.completeExceptionally(new UncheckedException("Execution refused"));
        }
        return completion;
    }

    protected abstract Task<?> handleOnMessageReceived(
            final Task completion,
            final EntryKey entryKey, final Message message);

}
