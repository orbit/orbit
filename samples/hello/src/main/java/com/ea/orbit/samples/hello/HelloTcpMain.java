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

package com.ea.orbit.samples.hello;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.cluster.ClusterPeer;
import com.ea.orbit.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HelloTcpMain
{
    public HelloTcpMain() throws InterruptedException {
        final String clusterName = "helloWorldTestCluster." + System.currentTimeMillis();
        OrbitStage stage1 = initStage(clusterName, "stage1");
        OrbitStage stage2 = initStage(clusterName, "stage2");
        System.out.println("Stages initialized");

        IHello helloFrom1 = IActor.getReference(IHello.class, "0");
        helloFrom1.register(new IHelloObserver() {
            @Override
            public Task<Void> receiveHello(String greeting) {
                System.out.println("Received: " + greeting);
                return Task.done();
            }
        });

        IHello helloFrom2 = IActor.getReference(IHello.class, "0");
        helloFrom2.register(new IHelloObserver() {
            @Override
            public Task<Void> receiveHello(String greeting) {
                System.out.println("Received: " + greeting);
                return Task.done();
            }
        });

        stage1.bind();
        System.out.println(helloFrom1.sayHello("Hi from 01").join());
        stage2.bind();
        System.out.println(helloFrom2.sayHello("Hi from 02").join());

        System.out.println();
        System.out.println("Type a message and press enter, or run other instances and see what happens.");
        System.out.print("-->");

        new BufferedReader(new InputStreamReader(System.in)).lines()
                .forEach(line -> {
                    System.out.println(helloFrom1.sayHello(line).join());
                    System.out.print("-->");
                });

        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new HelloTcpMain();
    }

    public static OrbitStage initStage(String clusterId, String stageId)
    {
        OrbitStage stage = new OrbitStage();
        stage.setClusterName(clusterId);
        ((ClusterPeer)stage.getClusterPeer()).setJgroupsConfig("classpath:/tcp.xml");
        stage.start().join();
        return stage;
    }

}

