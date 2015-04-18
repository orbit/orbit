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

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;

/**
 * Internal class to (when necessary) attach a java agent to
 * the jvm to instrument async-await methods.
 * <p/>
 * This class used internal "com.sun.tools" classes to talk to
 * talk to the jvm. This might not work with all jvms.
 * <p/>
 * When runtime agent attach is not possible use the "-javaagent:orbit-async-${project.version}.jar" option.
 *
 * @author Daniel Sperry
 */
public class AgentLoader
{
    static
    {
        if (!Transformer.initialized.isDone())
        {
            loadAgent();
        }
    }

    public static void loadAgent()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);

        final URL url = AgentLoader.class.getResource(AgentLoader.class.getSimpleName() + ".class");
        final String urlString = url.toString();

        final int idx = urlString.toLowerCase().indexOf(".jar!");
        final String jarName;
        try
        {
            if (idx > 0)
            {
                final int idx2 = urlString.lastIndexOf("file:/");
                jarName = new File(new URI(urlString.substring(idx2 >= 0 ? idx2 : 0, idx + 4))).getPath();
            }
            else
            {
                // test mode (or expanded jars mode)
                jarName = new File(AgentLoader.class.getResource("orbit-async-meta.jar").toURI()).getPath();
            }
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(jarName, "");
            vm.detach();
            while (!"true".equals(System.getProperty("orbit-async.running", "false")))
            {
                Thread.sleep(1);
            }
            //Transformer.initialized.join();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error activating orbit-async agent", e);
        }

    }


}
