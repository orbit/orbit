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

import java.lang.ref.WeakReference;

/**
 * Utility class to get the current ActorRuntime. Used by the generated code.
 */
public class RuntimeBinder
{
    /**
     * Last created runtime or at least the last surviving one to call setRuntime.
     */
    private static WeakReference<? extends BasicRuntime> lastRuntime;

    /**
     * Last runtime to touch the current thread.
     */
    private static final ThreadLocal<WeakReference<? extends BasicRuntime>> currentRuntime = new ThreadLocal<>();


    private RuntimeBinder()
    {
        // utility class
    }

    /**
     * Gets the current runtime, first looking at a thread local,
     * then at a global variable that contains the lasted runtime created
     *
     * @return the closest runtime to the current thread.
     */
    public static ActorRuntime getActorRuntime()
    {
        BasicRuntime runtime = getBasicRuntime();
        if (runtime instanceof ActorRuntime)
        {
            return (ActorRuntime) runtime;
        }

        WeakReference<? extends BasicRuntime> runtimeRef = currentRuntime.get();
        if (runtimeRef != null)
        {
            runtime = runtimeRef.get();
            if (runtime instanceof ActorRuntime)
            {
                return (ActorRuntime) runtime;
            }
        }
        if (lastRuntime != null)
        {
            runtime = lastRuntime.get();
            if (runtime instanceof ActorRuntime)
            {
                return (ActorRuntime) runtime;
            }
        }
        return null;
    }

    /**
     * Gets the current runtime, first looking at a thread local,
     * then at a global variable that contains the lasted runtime created
     *
     * @return the closest runtime to the current thread.
     */
    public static BasicRuntime getBasicRuntime()
    {
        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null)
        {
            final ActorRuntime runtime = context.getRuntime();
            if (runtime != null)
            {
                return runtime;
            }
        }
        final WeakReference<? extends BasicRuntime> runtimeRef = currentRuntime.get();
        BasicRuntime runtime;
        if (runtimeRef != null)
        {
            runtime = runtimeRef.get();
            if (runtime != null)
            {
                return runtime;
            }
        }
        if (lastRuntime != null)
        {
            runtime = lastRuntime.get();
            if (runtime != null)
            {
                return runtime;
            }
        }
        return null;
    }

    /**
     * Sets the runtime associated with the current thread.
     * <p>
     * It also tries to set the static runtime if the previous one got garbage collected.
     * </p>
     * <p> It's not necessary to "unset" the runtime because:
     * <ol>
     * <li>there should normally exist only one (only test cases are expected to have more than one)</li>
     * <li>it is already a weak reference</li>
     * <li>if another runtime is using the same thread later (shared thread pools) it will set the runtime before each usage</li>
     * </ol></p>
     *
     * @param runtimeRef a reference to the runtime
     */
    public static void setRuntime(final WeakReference<? extends BasicRuntime> runtimeRef)
    {
        if (runtimeRef.get() instanceof ActorRuntime)
        {
            final ActorTaskContext context = ActorTaskContext.current();
            if (context != null)
            {
                context.setRuntime((ActorRuntime) runtimeRef.get());
            }
        }
        currentRuntime.set(runtimeRef);
        lastRuntime = runtimeRef;
    }

}
