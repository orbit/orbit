package com.ea.orbit.actors.client.streams;


import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.concurrent.Task;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClientSideStreamProxy
{
    private Map<String, StreamSubscription> observerMap = new LinkedHashMap<>();

    static class StreamSubscription
    {
        String provider;
        Class dataClass;
        String streamId;
        boolean valid;
        Task<StreamSubscriptionHandle> handle;
    }

}
