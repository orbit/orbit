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

package com.ea.orbit.async.instrumentation;

import java.lang.instrument.Instrumentation;

/*
 * From https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/instrument/Instrumentation.html
 *
 * Agent-Class
 *
 * If an implementation supports a mechanism to start agents sometime
 * after the VM has started then this attribute specifies the agent class.
 * That is, the class containing the agentmain method. This attribute is
 * required, if it is not present the agent will not be started. Note: this
 * is a class name, not a file name or path.
 */
public class Agent
{


    public static void agentmain(String agentArgs, Instrumentation inst)
    {
        Transformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
//        f1:
//        for (Class<?> clazz : inst.getAllLoadedClasses())
//        {
//            if (inst.isModifiableClass(clazz))
//            {
//                try
//                {
//                    for (Method m : clazz.getDeclaredMethods())
//                    {
//                        if (m.isAnnotationPresent(Async.class))
//                        {
//                            inst.retransformClasses(clazz);
//                            continue f1;
//                        }
//                    }
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        }
        Transformer.running.complete(null);
    }
}
