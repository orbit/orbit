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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.actors.extensions.MessageSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

/**
 * A message serializer that uses java serialization.
 */
public class JavaMessageSerializer implements MessageSerializer
{
    @Override
    public Message deserializeMessage(final BasicRuntime runtime, final InputStream inputStream) throws Exception
    {
        final ObjectInput in = createObjectInput(runtime, inputStream);
        final Message message = new Message();
        message.setMessageType(in.readByte());
        message.setMessageId(in.readInt());
        long most = in.readLong();
        long least = in.readLong();
        message.setReferenceAddress(most != 0 && least != 0 ? new NodeAddressImpl(new UUID(most, least)) : null);
        message.setInterfaceId(in.readInt());
        message.setMethodId(in.readInt());
        message.setObjectId(in.readObject());
        message.setHeaders((Map) in.readObject());
        message.setFromNode((NodeAddress) in.readObject());
        message.setPayload(in.readObject());
        return message;
    }

    @Override
    public void serializeMessage(final BasicRuntime runtime, OutputStream outputStream, Message message) throws Exception
    {
        final ObjectOutput out = createObjectOutput(runtime, outputStream);
        out.writeByte(message.getMessageType());
        out.writeInt(message.getMessageId());
        if (message.getReferenceAddress() != null)
        {
            final UUID uuid = message.getReferenceAddress().asUUID();
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
        }
        else
        {
            out.writeLong(0);
            out.writeLong(0);
        }
        out.writeInt(message.getInterfaceId());
        out.writeInt(message.getMethodId());
        out.writeObject(message.getObjectId());
        out.writeObject(message.getHeaders());
        out.writeObject(message.getFromNode());
        out.writeObject(message.getPayload());
    }

    protected ObjectOutput createObjectOutput(final BasicRuntime runtime, final OutputStream outputStream) throws IOException
    {
        // Message(messageId, type, reference, params) and Message(messageId, type, object)
        return new OrbitObjectOutputStream(outputStream, runtime);
    }

    protected ObjectInput createObjectInput(final BasicRuntime runtime, InputStream in) throws IOException
    {
        return new OrbitObjectInputStream(in, runtime);
    }

}
