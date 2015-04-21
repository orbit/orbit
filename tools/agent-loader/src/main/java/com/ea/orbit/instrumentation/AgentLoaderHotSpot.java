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

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;

/**
 * Agent load for the hotspot virtual machine and virtual machines that provide con.sun.tools.attach.*
 * Don't use this class directly, prefer {@link AgentLoader} instead.
 */
public class AgentLoaderHotSpot implements AgentLoader.IAgentLoader
{
    /**
     * Dynamically loads a java agent.
     * Deals with the problem of finding the proper jvm classes.
     * Don't call this method directly use {@link com.ea.orbit.instrumentation.AgentLoader#loadAgent}  instead
     *
     * @param agentJar the agent jar
     * @param options  options that will be passed back to the agent, can be null
     * @see java.lang.instrument.Instrumentation
     */
    public void loadAgent(String agentJar, String options)
    {

        VirtualMachine vm = getVirtualMachine();
        if (vm == null)
        {
            throw new RuntimeException("Can't attach to this jvm. Add -javaagent:" + agentJar + " to the commandline");
        }
        try
        {
            try
            {
                vm.loadAgent(agentJar, options);
            }
            finally
            {
                vm.detach();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Can't attach to this jvm. Add -javaagent:" + agentJar + " to the commandline", e);
        }
    }

    public static VirtualMachine getVirtualMachine()
    {
        if (VirtualMachine.list().size() > 0)
        {
            // tools jar present
            String pid = getPid();
            try
            {
                return VirtualMachine.attach(pid);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        String jvm = System.getProperty("java.vm.name").toLowerCase();
        if (jvm.contains("hotspot"))
        {
            // tools jar not present, but it's a sun vm
            Class<VirtualMachine> virtualMachineClass = pickVmImplementation();
            try
            {
                final AttachProviderPlaceHolder attachProvider = new AttachProviderPlaceHolder();
                Constructor<VirtualMachine> vmConstructor = virtualMachineClass.getDeclaredConstructor(AttachProvider.class, String.class);
                vmConstructor.setAccessible(true);
                VirtualMachine newVM = vmConstructor.newInstance(attachProvider, getPid());
                return newVM;
            }
            catch (UnsatisfiedLinkError e)
            {
                throw new RuntimeException("This jre doesn't support the native library for attaching to the jvm", e);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // not a hotspot based virtual machine
        return null;
    }


    /**
     * Gets the current jvm pid.
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
     * Picks one of the Oracle's implementations of VirtualMachine
     */
    @SuppressWarnings("unchecked")
    private static Class<VirtualMachine> pickVmImplementation()
    {
        String os = System.getProperty("os.name").toLowerCase();
        try
        {
            if (os.contains("win"))
            {
                return (Class<VirtualMachine>) AgentLoaderHotSpot.class.getClassLoader().loadClass("sun.tools.attach.WindowsVirtualMachine");
            }
            if (os.contains("nix") || os.contains("nux") || os.indexOf("aix") > 0)
            {
                return (Class<VirtualMachine>) AgentLoaderHotSpot.class.getClassLoader().loadClass("sun.tools.attach.LinuxVirtualMachine");
            }
            if (os.contains("mac"))
            {
                return (Class<VirtualMachine>) AgentLoaderHotSpot.class.getClassLoader().loadClass("sun.tools.attach.BsdVirtualMachine");
            }
            if (os.contains("sunos") || os.contains("solaris"))
            {
                return (Class<VirtualMachine>) AgentLoaderHotSpot.class.getClassLoader().loadClass("sun.tools.attach.SolarisVirtualMachine");
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Can't find a vm implementation for the operational system: " + System.getProperty("os.name"));
    }

}
