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

package com.ea.orbit.instrumentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utilities to load java agents dynamically.
 * <p/>
 * Deals with the problem of finding the proper jvm classes.
 *
 * @author Daniel Sperry
 * @see java.lang.instrument.Instrumentation
 */
public class AgentLoader
{
    private volatile static AgentLoaderInterface agentLoader;

    /**
     * Internal class.
     */
    // used internally, has to be public to be accessible from the shadeClassLoaders
    interface AgentLoaderInterface
    {
        void loadAgent(String agentJar, String options);
    }

    /**
     * Dynamically loads a java agent.
     * Deals with the problem of finding the proper jvm classes.
     *
     * @param agentJar the agent jar
     * @param options  options that will be passed back to the agent, can be null
     * @see java.lang.instrument.Instrumentation
     */
    public static void loadAgent(String agentJar, String options)
    {
        AgentLoaderInterface agentLoader = getAgentLoader(agentJar);
        agentLoader.loadAgent(agentJar, options);
    }


    /**
     * Creates loads the agent class directly. The agent class must be visible from the system class loader.
     * <p/>
     * This method creates a temporary jar with the proper manifest and loads the agent using the jvm attach facilities.
     * <p/>
     * This will not work if the agent class can't be loaded by the system class loader.
     * <br>
     * This can be worked around like by adding the specific class and any other dependencies to the system class loader:
     * <pre><code>
     *     if(MyAgent.class.getClassLoader() != ClassLoader.getSystemClassLoader()) {
     *         ClassPathUtils.appendToSystemPath(ClassPathUtils.getClassPathFor(MyAgent.class));
     *         ClassPathUtils.appendToSystemPath(ClassPathUtils.getClassPathFor(OtherDepenencies.class));
     *     }
     *     loadAgent(MyAgent.class.getName(), null, null, true, true, false);
     * </code></pre>
     *
     * @param agentClassName the agent class name
     * @param options        options that will be passed back to the agent, can be null
     */
    public static void loadAgentClass(String agentClassName, String options)
    {
        loadAgentClass(agentClassName, options, null, true, true, false);
    }


    /**
     * Creates loads the agent class directly.
     * <p/>
     * This method creates a temporary jar with the proper manifest and loads the agent using the jvm attach facilities.
     * <p/>
     * This will not work if the agent class can't be loaded by the system class loader.
     * <br>
     * This can be worked around like by adding the specific class and any other dependencies to the system class loader:
     * <pre><code>
     *     if(MyAgent.class.getClassLoader() != ClassLoader.getSystemClassLoader()) {
     *         ClassPathUtils.appendToSystemPath(ClassPathUtils.getClassPathFor(MyAgent.class));
     *         ClassPathUtils.appendToSystemPath(ClassPathUtils.getClassPathFor(OtherDepenencies.class));
     *     }
     *     loadAgent(MyAgent.class.getName(), null, null, true, true, false);
     * </code></pre>
     *
     * @param agentClass               the agent class
     * @param options                  options that will be passed back to the agent, can be null
     * @param bootClassPath            list of jars to be loaded with the agent, can be null
     * @param canRedefineClasses       if the ability to redefine classes is need by the agent, suggested default: false
     * @param canRetransformClasses    if the ability to retransform classes is need by the agent, suggested default: false
     * @param canSetNativeMethodPrefix if the ability to set native method prefix is need by the agent, suggested default: false
     * @see ClassPathUtils
     * @see java.lang.instrument.Instrumentation
     */
    public static void loadAgentClass(
            final String agentClass,
            final String options,
            final String bootClassPath,
            final boolean canRedefineClasses,
            final boolean canRetransformClasses,
            final boolean canSetNativeMethodPrefix)
    {
        final File jarFile;
        try
        {
            jarFile = createTemporaryAgentJar(agentClass, bootClassPath, canRedefineClasses, canRetransformClasses, canSetNativeMethodPrefix);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Can't write jar file for agent:" + agentClass, ex);
        }
        loadAgent(jarFile.getPath(), options);
    }

    @SuppressWarnings("unchecked")
    private synchronized static AgentLoaderInterface getAgentLoader(final String agentJar)
    {
        if (agentLoader != null)
        {
            return agentLoader;
        }
        Class<AgentLoaderInterface> agentLoaderClass;
        try
        {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            agentLoaderClass = (Class<AgentLoaderInterface>) Class.forName("com.ea.orbit.instrumentation.AgentLoaderHotSpot");
        }
        catch (Exception ex)
        {
            // tools.jar not available in the class path
            // so we load our own copy of those files
            final List<String> shaded = Arrays.asList(
                    "shaded/AttachProvider.class",
                    "shaded/VirtualMachine.class",
                    "AttachProviderPlaceHolder.class",
                    "shaded/AgentInitializationException.class",
                    "shaded/AgentLoadException.class",
                    "shaded/AttachNotSupportedException.class",
                    "shaded/AttachOperationFailedException.class",
                    "shaded/AttachPermission.class",
                    "shaded/HotSpotAttachProvider.class",
                    "shaded/HotSpotVirtualMachine.class",
                    "shaded/VirtualMachineDescriptor.class",

                    "shaded/WindowsAttachProvider.class",
                    "shaded/WindowsVirtualMachine.class",

                    "shaded/SolarisAttachProvider.class",
                    "shaded/SolarisVirtualMachine.class",

                    "shaded/LinuxAttachProvider.class",
                    "shaded/LinuxVirtualMachine.class",

                    "shaded/BsdAttachProvider.class",
                    "shaded/BsdVirtualMachine.class",

                    "shaded/HotSpotAttachProvider$HotSpotVirtualMachineDescriptor.class",

                    "shaded/BsdVirtualMachine$SocketInputStream.class",
                    "shaded/LinuxVirtualMachine$SocketInputStream.class",
                    "shaded/SolarisVirtualMachine$SocketInputStream.class",
                    "shaded/WindowsVirtualMachine$PipedInputStream.class"
            );
            final ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            List<Class<?>> classes = new ArrayList<>();
            for (String s : shaded)
            {
                // have to load those class in the system class loader
                // to prevent getting "Native Library .../libattach.so already loaded in another classloader"
                // when the vm is used more than once.
                try
                {
                    classes.add(ClassPathUtils.defineClass(systemLoader, AgentLoader.class.getResourceAsStream(s)));
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error defining: " + s, e);
                }
            }
            try
            {
                agentLoaderClass = (Class<AgentLoaderInterface>) ClassPathUtils.defineClass(systemLoader, AgentLoader.class.getResourceAsStream("/com/ea/orbit/instrumentation/AgentLoaderHotSpot.class"));
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error loading AgentLoader implementation", e);
            }
        }
        try
        {
            Object agentLoaderObject = agentLoaderClass.newInstance();

            // the agent loader might be instantiated in another class loader
            // so no interface it implements is guaranteed to be visible here.
            // this reflection based implementation of this interface solves this problem.
            agentLoader = new AgentLoaderInterface()
            {
                @Override
                public void loadAgent(final String agentJar, final String options)
                {
                    try
                    {
                        final Method loadAgentMethod = agentLoaderObject.getClass().getMethod("loadAgent", String.class, String.class);
                        loadAgentMethod.invoke(agentLoaderObject, agentJar, options);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting agent loader implementation to load: " + agentJar, e);
        }
        return agentLoader;
    }

    /**
     * Gets the current jvm's pid.
     *
     * @return the pid as String
     */
    public static String getPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }

    /**
     * Creates a jar in runtime with the proper manifest file to start the javaagent.
     * <p/>
     * This method is convenient to java agent developers since they can test their agents without creating a jar first.
     *
     * @param agentClass               the agent class
     * @param bootClassPath            list of jars to be loaded with the agent, can be null
     * @param canRedefineClasses       if the ability to redefine classes is need by the agent, suggested default: false
     * @param canRetransformClasses    if the ability to retransform classes is need by the agent, suggested default: false
     * @param canSetNativeMethodPrefix if the ability to set native method prefix is need by the agent, suggested default: false
     */
    public static File createTemporaryAgentJar(
            final String agentClass,
            final String bootClassPath,
            final boolean canRedefineClasses,
            final boolean canRetransformClasses,
            final boolean canSetNativeMethodPrefix) throws IOException
    {
        final File jarFile = File.createTempFile("javaagent." + agentClass, ".jar");
        createAgentJar(new FileOutputStream(jarFile),
                agentClass,
                bootClassPath,
                canRedefineClasses,
                canRetransformClasses,
                canSetNativeMethodPrefix);
        return jarFile;
    }

    /**
     * Creates an agent jar with the proper manifest file to start a javaagent.
     *
     * @param agentClass               the agent class
     * @param bootClassPath            list of jars to be loaded with the agent, can be null
     * @param canRedefineClasses       if the ability to redefine classes is need by the agent, suggested default: false
     * @param canRetransformClasses    if the ability to retransform classes is need by the agent, suggested default: false
     * @param canSetNativeMethodPrefix if the ability to set native method prefix is need by the agent, suggested default: false
     */
    public static void createAgentJar(
            final OutputStream out,
            final String agentClass,
            final String bootClassPath,
            final boolean canRedefineClasses,
            final boolean canRetransformClasses,
            final boolean canSetNativeMethodPrefix) throws IOException
    {
        final Manifest man = new Manifest();
        man.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        man.getMainAttributes().putValue("Agent-Class", agentClass);
        if (bootClassPath != null)
        {
            man.getMainAttributes().putValue("Boot-Class-Path", bootClassPath);
        }
        man.getMainAttributes().putValue("Can-Redefine-Classes", Boolean.toString(canRedefineClasses));
        man.getMainAttributes().putValue("Can-Retransform-Classes", Boolean.toString(canRetransformClasses));
        man.getMainAttributes().putValue("Can-Set-Native-Method-Prefix", Boolean.toString(canSetNativeMethodPrefix));
        final JarOutputStream jarOut = new JarOutputStream(out, man);
        jarOut.flush();
        jarOut.close();
    }

}
