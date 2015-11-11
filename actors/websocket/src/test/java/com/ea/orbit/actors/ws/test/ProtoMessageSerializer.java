package com.ea.orbit.actors.ws.test;

import com.ea.orbit.actors.extensions.MessageSerializer;
import com.ea.orbit.actors.rpc.Msg;
import com.ea.orbit.actors.runtime.ActorInvoker;
import com.ea.orbit.actors.runtime.BasicRuntime;
import com.ea.orbit.actors.runtime.Message;
import com.ea.orbit.actors.runtime.MessageDefinitions;

import com.google.protobuf.CodedInputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ProtoMessageSerializer
        implements MessageSerializer
{

    @Override
    public Message deserializeMessage(final BasicRuntime runtime,
                                      final InputStream inputStream) throws Exception
    {
        final CodedInputStream input = CodedInputStream.newInstance(inputStream);

        final Msg.RpcMessage rpcMessage = Msg.RpcMessage.parseFrom(inputStream);
        final Message message = new Message();
        message.setMessageId(rpcMessage.getMessageId());
        message.setInterfaceId(rpcMessage.getInterfaceId());
        message.setMethodId(rpcMessage.getMethodId());
        message.setObjectId(rpcMessage.getObjectId());
        if (rpcMessage.getPayload() != null &&
                (message.getMessageType() == MessageDefinitions.ONE_WAY_MESSAGE
                        || message.getMessageType() == MessageDefinitions.REQUEST_MESSAGE))
        {
            final ActorInvoker invoker = runtime.getInvoker(message.getInterfaceId());
            final Method method = invoker.getMethod(message.getMethodId());

            final Type[] genericParameterTypes = method.getGenericParameterTypes();
            Object[] casted = new Object[genericParameterTypes.length];
            for (int i = 0; i < genericParameterTypes.length; i++)
            {
                final CodedInputStream stream = CodedInputStream.newInstance(rpcMessage.getPayload().asReadOnlyByteBuffer());
                final int tag = stream.readTag();
                casted[(tag>>3)-1] = stream.readString();
            }
            message.setPayload(casted);
        }
        return message;
    }


    @Override
    public void serializeMessage(final BasicRuntime runtime,
                                 OutputStream out,
                                 Message message) throws Exception
    {
        final Msg.RpcMessage rpcMessage = Msg.RpcMessage.newBuilder()
                .setMessageId(message.getMessageId())
                .setInterfaceId(message.getInterfaceId())
                .setMethodId(message.getMethodId())
                .setObjectId((String) message.getObjectId())
                .build();

        rpcMessage.writeDelimitedTo(out);
    }
}
