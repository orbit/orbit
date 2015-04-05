/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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

package com.ea.orbit.samples.helloworld;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.samples.hello.IHello;

import org.junit.Test;

public class HelloTest
{
    @Test
    public void test()
    {
        final String clusterName = "helloWorldTestCluster." + System.currentTimeMillis();
        OrbitStage stage1 = initStage(clusterName, "stage1");
        OrbitStage stage2 = initStage(clusterName, "stage2");
        System.out.println("Stages initialized");

        IHello helloFrom1 = IActor.getReference(IHello.class, "0");
        IHello helloFrom2 = IActor.getReference(IHello.class, "0");

        stage1.bind();
        System.out.println(helloFrom1.sayHello("Hi from 01").join());
        stage2.bind();
        System.out.println(helloFrom2.sayHello("Hi from 02").join());
    }

    public static OrbitStage initStage(String clusterId, String stageId)
    {
        OrbitStage stage = new OrbitStage();
        stage.setClusterName(clusterId);
        stage.start().join();
        return stage;
    }
}

