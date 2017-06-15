/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.interceptable;

import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;
import cloud.orbit.actors.extensions.interceptor.MessageInterceptor;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.Message;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class InterceptableMessageSerializer extends InterceptableActorExtension<MessageInterceptor, MessageSerializer>
        implements MessageSerializer
{
    public InterceptableMessageSerializer(final MessageSerializer extension,
                                          final List<MessageInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableMessageSerializer(final MessageSerializer extension)
    {
        super(extension);
    }

    @Override
    public Message deserializeMessage(final BasicRuntime runtime, final InputStream inputStream) throws Exception
    {
        if (!hasInterceptors())
        {
            return extension.deserializeMessage(runtime, inputStream);
        }
        InterceptedValue<BasicRuntime> interceptedRuntime = InterceptedValue.of(runtime);
        InterceptedValue<InputStream> interceptedInputStream = InterceptedValue.of(inputStream);
        intercept(interceptor -> interceptor.preDeserializeMessage(interceptedRuntime, interceptedInputStream));
        InterceptedValue<Message> interceptedReturnValue = InterceptedValue.of(
                extension.deserializeMessage(interceptedRuntime.get(), interceptedInputStream.get()));
        intercept(interceptor ->
                interceptor.postDeserializeMessage(interceptedRuntime, interceptedInputStream, interceptedReturnValue));
        return interceptedReturnValue.get();
    }

    @Override
    public void serializeMessage(final BasicRuntime runtime, final OutputStream out, final Message message)
            throws Exception
    {
        if (!hasInterceptors())
        {
            extension.serializeMessage(runtime, out, message);
            return;
        }
        InterceptedValue<BasicRuntime> interceptedRuntime = InterceptedValue.of(runtime);
        InterceptedValue<OutputStream> interceptedOut = InterceptedValue.of(out);
        InterceptedValue<Message> interceptedMessage = InterceptedValue.of(message);
        intercept(interceptor ->
                interceptor.preSerializeMessage(interceptedRuntime, interceptedOut, interceptedMessage));
        extension.serializeMessage(interceptedRuntime.get(), interceptedOut.get(), interceptedMessage.get());
        intercept(interceptor ->
                interceptor.postSerializeMessage(interceptedRuntime, interceptedOut, interceptedMessage));
    }
}
