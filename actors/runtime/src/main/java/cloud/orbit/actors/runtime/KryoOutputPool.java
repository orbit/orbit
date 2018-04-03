/*
 Copyright (C) 2018 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.runtime;

import com.esotericsoftware.kryo.io.Output;

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
class KryoOutputPool
{
    private static final int DEFAULT_MAX_POOLED_BUFFER_SIZE = 512 * 1024;
    private static final int DEFAULT_MAX_BUFFER_SIZE = 768 * 1024;

    private final int maxPooledBufferSize = DEFAULT_MAX_POOLED_BUFFER_SIZE;
    private final int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;

    private final Queue<SoftReference<Output>> queue = new ConcurrentLinkedQueue<>();

    public <R> R run(final Function<Output, R> callback, final int bufferSize)
    {
        final Output element = borrow(bufferSize);
        try
        {
            return callback.apply(element);
        }
        finally
        {
            release(element);
        }
    }

    private Output create(final int bufferSize)
    {
        return new Output(bufferSize, maxBufferSize);
    }

    private Output borrow(final int bufferSize)
    {
        Output output;
        SoftReference<Output> reference;
        while ((reference = queue.poll()) != null)
        {
            if ((output = reference.get()) != null)
            {
                return output;
            }
        }
        return create(bufferSize);
    }

    private void release(Output output)
    {
        if (output.getBuffer().length < maxPooledBufferSize)
        {
            output.clear();
            queue.offer(new SoftReference<>(output));
        }
    }
}