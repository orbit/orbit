package com.ea.orbit.actors.test.transactions;

public class TransactionEvent
{
    public String getTransactionId()
    {
        return transactionId;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public Object[] getParams()
    {
        return params;
    }

    private String transactionId;
    private final String methodName;
    private final Object[] params;

    public TransactionEvent(String transactionId, String methodName, Object... params)
    {
        this.transactionId = transactionId;
        this.methodName = methodName;
        this.params = params;
    }
}
