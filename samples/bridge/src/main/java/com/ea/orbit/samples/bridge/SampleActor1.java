package com.ea.orbit.samples.bridge;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;

public class SampleActor1 extends OrbitActor implements ISample1 {
    @Override
    public Task<Integer> sum(int a, int b) {
        return Task.fromValue(a+b);
    }
}
