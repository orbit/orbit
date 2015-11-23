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
 */package com.ea.orbit.profiler;

import com.ea.orbit.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the profile data for one profile key.
 * Keys can be threads, application processes, or global for the entire system.
 * <p/>
 * The profile data is not thread safe. The profile collection should be paused while reading it.
 * @author Daniel Sperry
 */
public class ProfilerData
{
    private CallTreeElement root = new CallTreeElement(new MethodInfo(null));
    private final HashMap<MethodInfo, MethodInfo> all = new HashMap<>();

    private long currentStamp = (long) (Math.random() * Long.MAX_VALUE);

    public static class MethodInfo
    {
        private String declaringClass;
        private String methodName;
        private String fileName;
        private long count;
        public long stamp;

        public MethodInfo()
        {

        }

        public MethodInfo(final StackTraceElement e)
        {
            if (e != null)
            {
                this.declaringClass = e.getClassName();
                this.methodName = e.getMethodName();
                this.fileName = e.getFileName();
            }
            else
            {
                this.declaringClass = "<root>";
                this.methodName = "<root>";
                this.fileName = "<root>";
            }
        }

        public String getDeclaringClass()
        {
            return declaringClass;
        }

        public String getMethodName()
        {
            return methodName;
        }

        public String getFileName()
        {
            return fileName;
        }

        public long getCount()
        {
            return count;
        }

        @Override
        public boolean equals(final Object obj)
        {
            final MethodInfo other = (MethodInfo) obj;
            return StringUtils.equals(this.declaringClass, other.declaringClass)
                    && StringUtils.equals(this.methodName, other.methodName);
        }

        @Override
        public int hashCode()
        {
            return this.declaringClass.hashCode()
                    + (this.methodName != null ? this.methodName.hashCode() : 0) * 31;
        }
    }

    public static class CallTreeElement
    {
        private MethodInfo elementInfo;
        private Map<MethodInfo, CallTreeElement> children = new HashMap<>();
        private long count;

        public CallTreeElement()
        {
        }

        public CallTreeElement(final MethodInfo elementInfo)
        {
            this.elementInfo = elementInfo;
        }
    }

    public void collect(final StackTraceElement[] stack)
    {
        root.elementInfo.count++;
        root.count++;
        CallTreeElement current = root;
        currentStamp++;

        MethodInfo probe = new MethodInfo();
        for (int i = stack.length, k = 0; --i >= 0 && k < 256; k++)
        {
            final StackTraceElement s = stack[i];

            probe.declaringClass = s.getClassName();
            probe.methodName = s.getMethodName();
            probe.fileName = s.getFileName();

            // counts the number of times a method appear
            MethodInfo e = all.get(probe);
            if (e == null)
            {
                e = new MethodInfo(s);
                all.put(e, e);
            }
            // this prevents counting recursive methods twice
            if (e.stamp != currentStamp)
            {
                e.count++;
                e.stamp = currentStamp;
            }

            // builds a call tree
            CallTreeElement ce = current.children.get(e);
            if (ce == null)
            {
                current.children.putIfAbsent(e, new CallTreeElement(e));
                ce = current.children.get(e);
            }
            ce.count++;
            current = ce;
        }
    }

    public void reset()
    {
        root = new CallTreeElement(new MethodInfo(null));
        all.clear();
    }

    public Set<MethodInfo> getMethods()
    {
        return all.keySet();
    }

    public CallTreeElement getCallTree()
    {
        return root;
    }

}
