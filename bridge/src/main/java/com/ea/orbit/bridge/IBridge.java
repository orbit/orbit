package com.ea.orbit.bridge;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.annotation.NoIdentity;
import com.ea.orbit.concurrent.Task;

@NoIdentity
public interface IBridge extends IActor{

    Task<BridgeResult> call(String url, Class responseClass);

}
