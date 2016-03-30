/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.profiler;

import cloud.orbit.util.StringUtils;

public class MethodInfo
{
    String declaringClass;
    String methodName;
    String fileName;
    long count;
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

    // todo remote this equals
    @Override
    public boolean equals(final Object obj)
    {
        // this equals may cause problems for external users
        // since it assumes a certain use case...
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

    public void setDeclaringClass(final String declaringClass)
    {
        this.declaringClass = declaringClass;
    }

    public void setMethodName(final String methodName)
    {
        this.methodName = methodName;
    }

    public void setFileName(final String fileName)
    {
        this.fileName = fileName;
    }

    public void setCount(final long count)
    {
        this.count = count;
    }

    public long getStamp()
    {
        return stamp;
    }

    public void setStamp(final long stamp)
    {
        this.stamp = stamp;
    }
}
