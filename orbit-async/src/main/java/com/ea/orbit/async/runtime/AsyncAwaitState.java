/*
Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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

package com.ea.orbit.async.runtime;

/**
 * Internal class to hold the locals and stack of an async-await method
 * while the method is awaiting for a future.
 */
public class AsyncAwaitState
{
    private final Object[] locals;
    private int pos;
    private int top;

    public AsyncAwaitState(int pos, int localsSize, int stackSize)
    {
        locals = new Object[localsSize + stackSize];
    }

    public static AsyncAwaitState push(final int val, AsyncAwaitState state)
    {
        state.locals[state.top++] = val;
        return state;
    }

    public static AsyncAwaitState push(final long val, AsyncAwaitState state)
    {
        state.locals[state.top++] = val;
        return state;
    }

    public static AsyncAwaitState push(final float val, AsyncAwaitState state)
    {
        state.locals[state.top++] = val;
        return state;
    }

    public static AsyncAwaitState push(final double val, AsyncAwaitState state)
    {
        state.locals[state.top++] = val;
        return state;
    }

    public static AsyncAwaitState push(final Object val, AsyncAwaitState state)
    {
        state.locals[state.top++] = val;
        return state;
    }

    public AsyncAwaitState push(final int val)
    {
        this.locals[this.top++] = val;
        return this;
    }

    public AsyncAwaitState push(final long val)
    {
        this.locals[this.top++] = val;
        return this;
    }

    public AsyncAwaitState push(final float val)
    {
        this.locals[this.top++] = val;
        return this;
    }

    public AsyncAwaitState push(final double val)
    {
        this.locals[this.top++] = val;
        return this;
    }

    public AsyncAwaitState push(final Object val)
    {
        this.locals[this.top++] = val;
        return this;
    }

    public int getI(int i)
    {
        return (Integer) locals[i];
    }

    public long getJ(int i)
    {
        return (Long) locals[i];
    }

    public float getF(int i)
    {
        return (Float) locals[i];
    }

    public double getD(int i)
    {
        return (Double) locals[i];
    }

    public Object getObj(int i)
    {
        return locals[i];
    }

    public int getPos()
    {
        return pos;
    }


}
