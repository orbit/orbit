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

package cloud.orbit.concurrent;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Concurrent hash set with an internal java.util.ConcurrentHashMap
 *
 * @param <T> the element type
 */
public class ConcurrentHashSet<T>
        extends AbstractSet<T>
        implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final ConcurrentHashMap<T, Boolean> m;
    private transient Set<T> s;

    public ConcurrentHashSet()
    {
        m = new ConcurrentHashMap<>();
        s = m.keySet();
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        s = m.keySet();
    }

    @Override
    public Iterator<T> iterator()
    {
        return s.iterator();
    }

    @Override
    public int size()
    {
        return m.size();
    }

    public void clear()
    {
        m.clear();
    }

    public boolean isEmpty()
    {
        return m.isEmpty();
    }

    public boolean contains(Object o)
    {
        return m.containsKey(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        return s.containsAll(c);
    }

    @Override
    public void forEach(Consumer<? super T> action)
    {
        s.forEach(action);
    }

    @Override
    public Stream<T> stream()
    {
        return s.stream();
    }

    @Override
    public Stream<T> parallelStream()
    {
        return s.parallelStream();
    }

    @Override
    public Spliterator<T> spliterator()
    {
        return s.spliterator();
    }


    public Object[] toArray()
    {
        return s.toArray();
    }

    public <R> R[] toArray(R[] a)
    {
        return s.toArray(a);
    }

    public boolean equals(Object o)
    {
        return o == this || s.equals(o);
    }

    public int hashCode()
    {
        return s.hashCode();
    }

    public boolean add(T e)
    {
        return m.put(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter)
    {
        return s.removeIf(filter);
    }

    public boolean remove(Object o)
    {
        return m.remove(o) != null;
    }

    public boolean removeAll(Collection<?> c)
    {
        return s.removeAll(c);
    }


    public boolean retainAll(Collection<?> c)
    {
        return s.retainAll(c);
    }

    public String toString()
    {
        return s.toString();
    }
}

