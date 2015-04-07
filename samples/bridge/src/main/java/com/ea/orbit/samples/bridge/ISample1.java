package com.ea.orbit.samples.bridge;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.bridge.Bridge;
import com.ea.orbit.concurrent.Task;

@StatelessWorker
public interface ISample1 extends IActor {

    @Bridge(path="/sample1/{id}/sum/{arg0}/{arg1}")
    Task<Integer> sum(int a,int b);

}
