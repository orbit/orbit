/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.interceptable;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.NodeSelectorExtension;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;
import cloud.orbit.actors.extensions.interceptor.NodeSelectionInterceptor;
import cloud.orbit.actors.runtime.NodeInfo;

import java.util.List;

public class InterceptableNodeSelectorExtension
        extends InterceptableActorExtension<NodeSelectionInterceptor, NodeSelectorExtension>
        implements NodeSelectorExtension
{
    public InterceptableNodeSelectorExtension(final NodeSelectorExtension extension,
                                              final List<NodeSelectionInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableNodeSelectorExtension(final NodeSelectorExtension extension)
    {
        super(extension);
    }

    @Override
    public NodeInfo select(final String interfaceClassName,
                           final NodeAddress localAddress,
                           final List<NodeInfo> potentialNodes)
    {
        if (!hasInterceptors())
        {
            return extension.select(interfaceClassName, localAddress, potentialNodes);
        }
        InterceptedValue<String> interceptedInterfaceClassName = InterceptedValue.of(interfaceClassName);
        InterceptedValue<NodeAddress> interceptedLocalAddress = InterceptedValue.of(localAddress);
        InterceptedValue<List<NodeInfo>> interceptedPotentialNodes = InterceptedValue.of(potentialNodes);
        intercept(interceptor -> interceptor.preSelect(
                interceptedInterfaceClassName, interceptedLocalAddress, interceptedPotentialNodes));
        InterceptedValue<NodeInfo> interceptedReturnValue = InterceptedValue.of(extension.select(
                interceptedInterfaceClassName.get(), interceptedLocalAddress.get(), interceptedPotentialNodes.get()));
        intercept(interceptor -> interceptor.postSelect(interceptedInterfaceClassName, interceptedLocalAddress,
                interceptedPotentialNodes, interceptedReturnValue));
        return interceptedReturnValue.get();
    }
}
