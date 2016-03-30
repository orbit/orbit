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

package cloud.orbit.reflect;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClassCache
{
    private WeakHashMap<Class<?>, ClassDescriptor<?>> map = new WeakHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    public static final ClassCache shared = new ClassCache();

    @SuppressWarnings("unchecked")
	public <T> ClassDescriptor<T> getClass(Class<T> clazz)
    {
        ClassDescriptor<?> desc;
        lock.readLock().lock();
        try
        {
            desc = map.get(clazz);
        }
        finally
        {
            lock.readLock().unlock();
        }
        if (desc != null)
        {
            return (ClassDescriptor<T>) desc;
        }
        else
        {
            desc = createClassDescriptor(clazz);
            lock.writeLock().lock();
            try
            {
                map.put(clazz, desc);
            }
            finally
            {
                lock.writeLock().unlock();
            }
            return (ClassDescriptor<T>) desc;
        }
    }

    protected <T> ClassDescriptor<?> createClassDescriptor(Class<T> clazz)
    {
        return new ClassDescriptor<T>(this, clazz);
    }
}
