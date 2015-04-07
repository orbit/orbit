package com.ea.orbit.samples.bridge;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.annotation.NoIdentity;
import com.ea.orbit.bridge.Bridge;
import com.ea.orbit.concurrent.Task;

@NoIdentity
public interface ISample2 extends IActor {

    @Bridge(path="/sample2/buy/{arg0}")
    Task<SampleData> buyItem(String itemId);

    //this call will use bridge client to get the information from another site
    @Bridge(path="/sample2/getblogpost/{arg0}")
    Task<SampleBlogPost> getblogpost(int postNumber);

}
