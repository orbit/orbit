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
package cloud.orbit.actors.transactions;

public class TransactionEvent
{
    // to be used to merge split clusters
    // should be locally (node) monotonic
    private long timestamp;

    private String transactionId;
    private String methodName;
    private Object[] params;

    public TransactionEvent(
            final long timestamp,
            final String transactionId,
            final String methodName,
            final Object[] params)
    {
        this.timestamp = timestamp;
        this.transactionId = transactionId;
        this.methodName = methodName;
        this.params = params;
    }

    public TransactionEvent()
    {
    }

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

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(final long timestamp)
    {
        this.timestamp = timestamp;
    }

    public void setTransactionId(final String transactionId)
    {
        this.transactionId = transactionId;
    }

    public void setMethodName(final String methodName)
    {
        this.methodName = methodName;
    }

    public void setParams(final Object[] params)
    {
        this.params = params;
    }

}
