package com.ea.orbit.samples.bridge;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.bridge.BridgeResult;
import com.ea.orbit.bridge.IBridge;
import com.ea.orbit.concurrent.Task;

public class SampleActor2 extends OrbitActor implements ISample2 {

    @Override
    public Task<SampleData> buyItem(String itemId) {
        SampleData data = new SampleData();
        data.item = itemId;
        data.someValue=42;
        data.anotherValue="show";
        return Task.fromValue(data);
    }

    @Override
    public Task<SampleBlogPost> getblogpost(int postNumber) {
        IBridge bridgeClient = IActor.getReference(IBridge.class);
        BridgeResult result = bridgeClient.call("http://jsonplaceholder.typicode.com/posts/" + postNumber, SampleBlogPost.class).join();
        return Task.fromValue((SampleBlogPost)result.value);
    }
}
