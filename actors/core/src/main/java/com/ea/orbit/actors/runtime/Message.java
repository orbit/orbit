package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.cluster.NodeAddress;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a message being sent between orbit servers.
 * Messages may be requests, responses or oneway.
 *
 * Header and body serialization is handled by the message serializer.
 */
public class Message
{
    private int messageType;
    private int messageId;
    private NodeAddress fromNode;
    private NodeAddress toNode;
    private Map<Object, Object> headers;
    private Object payload;
    private int interfaceId;
    private int methodId;
    private Object objectId;

    public Message()
    {
    }

    public int getInterfaceId()
    {
        return interfaceId;
    }

    public void setInterfaceId(final int interfaceId)
    {
        this.interfaceId = interfaceId;
    }

    public Message withInterfaceId(final int interfaceId)
    {
        this.interfaceId = interfaceId;
        return this;
    }

    public int getMethodId()
    {
        return methodId;
    }

    public void setMethodId(final int methodId)
    {
        this.methodId = methodId;
    }

    public Message withMethodId(final int methodId)
    {
        this.methodId = methodId;
        return this;
    }

    public Object getObjectId()
    {
        return objectId;
    }

    public void setObjectId(final Object objectId)
    {
        this.objectId = objectId;
    }

    public Message withObjectId(final Object objectId)
    {
        this.objectId = objectId;
        return this;
    }

    public int getMessageId()
    {
        return messageId;
    }

    public void setMessageId(final int messageId)
    {
        this.messageId = messageId;
    }

    public Message withMessageId(int messageId)
    {
        this.messageId = messageId;
        return this;
    }

    public Object getHeader(Object key)
    {
        return headers != null ? headers.get(key) : null;
    }

    public void setHeader(Object key, Object value)
    {
        if (headers == null)
        {
            headers = new LinkedHashMap<>();
        }
        headers.put(key, value);
    }

    public Message withHeader(Object key, Object value)
    {
        if (headers == null)
        {
            headers = new LinkedHashMap<>();
        }
        headers.put(key, value);
        return this;
    }

    public Map<Object, Object> getHeaders()
    {
        return headers;
    }

    public Message withHeaders(final Map<Object, Object> headers)
    {
        this.headers = headers;
        return this;
    }

    public void setHeaders(final Map<Object, Object> headers)
    {
        this.headers = headers;
    }

    public int getMessageType()
    {
        return messageType;
    }

    public void setMessageType(final int messageType)
    {
        this.messageType = messageType;
    }

    public NodeAddress getFromNode()
    {
        return fromNode;
    }

    public void setFromNode(final NodeAddress fromNode)
    {
        this.fromNode = fromNode;
    }

    public Object getPayload()
    {
        return payload;
    }

    public void setPayload(final Object payload)
    {
        this.payload = payload;
    }

    public Message withPayload(final Object payload)
    {
        this.payload = payload;
        return this;
    }

    public Message withMessageType(final int messageType)
    {
        this.messageType = messageType;
        return this;
    }

    public NodeAddress getToNode()
    {
        return toNode;
    }

    public void setToNode(final NodeAddress toNode)
    {
        this.toNode = toNode;
    }

    public Message withToNode(final NodeAddress toNode)
    {
        this.toNode = toNode;
        return this;
    }

    public Message withFromNode(final NodeAddress fromNode)
    {
        this.fromNode = fromNode;
        return this;
    }


}
