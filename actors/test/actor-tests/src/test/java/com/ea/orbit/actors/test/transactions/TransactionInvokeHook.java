package com.ea.orbit.actors.test.transactions;

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.extensions.InvocationContext;
import com.ea.orbit.actors.extensions.InvokeHookExtension;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.concurrent.Task;

import java.lang.reflect.Method;

public class TransactionInvokeHook implements InvokeHookExtension
{
    public Task<?> invoke(InvocationContext icontext, Addressable toReference, Method method, int methodId, Object[] params)
    {
        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null)
        {
            String tid = AbstractTransactionalActor.currentTransactionId(context);
            if (tid != null)
            {
                if(context.getActor()!=null) {

                }
            }
        }
        return icontext.invokeNext(toReference, method, methodId, params);
    }
}
