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

package cloud.orbit.actors;


import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.concurrent.Task;

/**
 * Used to create interfaces to client objects that can be passed as remote references to actors.
 * <p>
 * Observers will receive remote messages like actors do.
 * However, since they are managed by the application they are tied to the lifecycle
 * of the cluster node or client where they exist.</p>
 * <p>
 * When a client calls an actor method passing a observer reference.
 * The framework will create a special remote reference to that observer that will be callable
 * from any actor in the cluster.
 * </p>
 * <p>The framework refers to the observers using weak references. So it's the applications
 * job to hold a hard reference to the observers.
 * Otherwise they will be garbage collected and future messages to them will start failing</p>
 * <p>
 * The actor are allowed to hold and persist references to these client objects.
 * </p>
 * <p>
 * Since the observers are not indestructible, it is to be expected that references to ActorObservers may become invalid.
 * The ping() method exists to allow application code to validate if the observer is still alive.
 * </p>
 * <p>
 * References to observers should be managed using ObserverManager.
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 *  public interface ChatMember extends ActorObserver
 *  {
 *      {@literal@}OneWay
 *      Task&lt;Void&gt; receiveChatMessage(String message);
 *  }
 *
 *  public void Chat extends OrbitActor implements IChat
 *  {
 *     private ObserverManager observers;
 *
 *     public Task&lt;Void&gt; addListener(IChatMember member)
 *     {
 *          observers.addMember(observer);
 *          return Task.done();
 *     }
 *
 *     public Task&lt;Void&gt; say(String message)
 *     {
 *          observers.notifyAll( o -> o.receiveChatMessage(message) );
 *          return Task.done();
 *     }
 * }
 *
 * IChatMember member1 = new IChatMember() {
 *     public Task&lt;Void&gt; receiveChatMessage(String message) {
 *          System.out.println(message);
 *     }
 * };
 *
 * IChat chat = ChatFactory.createReference("dragon-age");
 * chat.addMember(member1).join();
 * chat.say("Hello!").join();
 * </pre>
 * </p>
 */
public interface ActorObserver
{
    /**
     * Allows the application to verify if the observer is still alive.
     * Used by {@code ObserverManager.cleanup()}
     */
    default Task<?> ping()
    {
        return Task.done();
    }

    /**
     * Gets a reference to a remote observer.
     *
     * @param node           the node address
     * @param actorObserverInterface the interface
     * @param id             the observer id
     * @param <T>            the interface type
     * @return an actor reference
     */
    static <T extends ActorObserver> T getObserverReference(NodeAddress node, Class<T> actorObserverInterface, String id)
    {
        return DefaultDescriptorFactory.observerRef(node, actorObserverInterface, id);
    }
}
