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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Internal class to (when necessary) attach a java agent to
 * the jvm to instrument async-await methods.
 * <p/>
 * This class uses internal "com.sun.tools" classes to talk to
 * talk to the jvm. This might not work with all jvms.
 * <p/>
 * When runtime agent attachment is not possible use the "-javaagent:orbit-async-${project.version}.jar" jvm option.
 *
 * @author Daniel Sperry
 */
class AgentLoader
{
    static
    {
        // yes, a "global" variable.
        // it's hard to escape this when the system class loader
        // is different from the application class loader.
        if (!"true".equals(System.getProperty(InitializeAsync.ORBIT_ASYNC_RUNNING, "false")))
        {
            loadAgent();
        }
        // else: being here means that probably the agent was started it the -javaagent option
    }

    static URL getClassPathFor(Class<?> clazz) throws URISyntaxException, MalformedURLException
    {
        URLClassLoader cc;

        final URL url = clazz.getResource(clazz.getSimpleName().replace('.', '$') + ".class");
        String urlString = url.toString();
        final int idx = urlString.toLowerCase().indexOf(".jar!");
        if (idx > 0)
        {
            final int idx2 = urlString.lastIndexOf("file:/");
            return new URL(urlString.substring(idx2 >= 0 ? idx2 : 0, idx + 4));
        }
        else
        {
            File dir = new File(url.toURI()).getParentFile();
            if (clazz.getPackage() != null)
            {
                String pn = clazz.getPackage().getName();
                for (int i = pn.indexOf('.'); i >= 0; i = pn.indexOf('.', i + 1))
                {
                    dir = dir.getParentFile();
                }
                dir = dir.getParentFile();
            }
            return dir.toURI().toURL();
        }
    }

    static void loadAgent()
    {
        String jarName = null;
        try
        {
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            int p = nameOfRunningVM.indexOf('@');
            String pid = nameOfRunningVM.substring(0, p);

            final URL url = getClassPathFor(AgentLoader.class);
            final File classPathFile = new File(url.toURI());
            if (classPathFile.isFile())
            {
                jarName = classPathFile.getPath();
            }
            else
            {
                // test mode (or expanded jars mode)
                // this happens during "mvn test" in the project dir
                // or if someone expanded the jars
                File jarFile = new File(AgentLoader.class.getResource("orbit-async-meta.jar").toURI());
                jarName = jarFile.getPath();

                ClassLoader mainAppLoader = ClassLoader.getSystemClassLoader();
                if (mainAppLoader != AgentLoader.class.getClassLoader())
                {
                    // this happens during "mvn test" from the parent project
                    final URL agentClassPath = getClassPathFor(AgentLoader.class);
                    final URL asmClassPath = getClassPathFor(ClassVisitor.class);
                    final URL asmClassPath2 = getClassPathFor(ClassNode.class);
                    final URL asmClassPath3 = getClassPathFor(Analyzer.class);
                    try
                    {
                        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{ URL.class });
                        method.setAccessible(true);
                        method.invoke(mainAppLoader, agentClassPath);
                        method.invoke(mainAppLoader, asmClassPath);
                        method.invoke(mainAppLoader, asmClassPath2);
                        method.invoke(mainAppLoader, asmClassPath3);
                    }
                    catch (Exception ex)
                    {
                        throw new IllegalArgumentException("Add URL failed: " + agentClassPath, ex);
                    }
                }
            }
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(jarName, "");
            vm.detach();

            // In scenarios were the application class loader is not the system class loader
            // we can't share application classes with the agent, since it they would just
            // be loaded again by the system class loader.
            //
            // using System.properties is ugly but solves the problem of sharing data.
            //
            // keep in mind that this loop happens only once in the entire application lifecycle.
            // and during the static initialization of the class Await.
            while (!"true".equals(System.getProperty(InitializeAsync.ORBIT_ASYNC_RUNNING, "false")))
            {
                Thread.sleep(1);
            }
        }
        catch (Throwable e)
        {
            if (jarName != null)
            {
                throw new RuntimeException("Error activating orbit-async agent from " + jarName, e);
            }
            else
            {
                throw new RuntimeException("Error activating orbit-async agent", e);
            }
        }
    }
}
