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

package com.ea.orbit.samples.scala

import com.ea.orbit.actors.runtime.OrbitActor
import com.ea.orbit.actors.{IActor, OrbitStage}
import com.ea.orbit.concurrent.Task

object Main {

  val CLUSTER_NAME = "trilean"

  def main(args: Array[String]): Unit = {
    val stage1 = initStage(CLUSTER_NAME, "stage1")
    val stage2 = initStage(CLUSTER_NAME, "stage2")

    val helloFrom1 = IActor.getReference(classOf[IHello], "0")
    val helloFrom2 = IActor.getReference(classOf[IHello], "0")

    stage1.bind()
    println(helloFrom1.sayHello("Hi from 01").join())
    stage2.bind()
    println(helloFrom2.sayHello("Hi from 02").join())
    stage1.stop().join()
    stage2.stop().join()
  }

  def initStage(clusterId: String, stageId: String) = {
    val stage = new OrbitStage()
    stage.setClusterName(clusterId)
    stage.addProvider("com.ea.orbit.samples.scala.*")
    stage.start().join()
    stage
  }

}

trait IHello extends IActor {
  def sayHello(greeting: String): Task[String]
}

class HelloActor extends OrbitActor[AnyRef] with IHello {

  def sayHello(greeting: String): Task[String] = {
    getLogger.info("Here: " + greeting)
    Task.fromValue("You said: '" + greeting + "', I say: Hello from " + System.identityHashCode(this) + " !")
  }

}
