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

package cloud.orbit.actors.test.dto;

import cloud.orbit.actors.test.actors.CacheResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestDto1 implements Serializable
{
    private TestDto2 dto2;
    private int sampleInt;
    private UUID id;
    private CacheResponse sampleActor;
    private List<Integer> sampleIntList = new ArrayList<>();
    private List<Integer> sampleIntList2 = Collections.unmodifiableList(Arrays.asList(1, 3, 3, 7));

    public TestDto1()
    {
        id = UUID.randomUUID();
    }

    public TestDto2 getDto2()
    {
        return dto2;
    }

    public void setDto2(TestDto2 dto2)
    {
        this.dto2 = dto2;
    }

    public int getSampleInt()
    {
        return sampleInt;
    }

    public void setSampleInt(int sampleInt)
    {
        this.sampleInt = sampleInt;
    }

    public UUID getId()
    {
        return id;
    }

    public CacheResponse getSampleActor()
    {
        return sampleActor;
    }

    public void setSampleActor(CacheResponse sampleActor)
    {
        this.sampleActor = sampleActor;
    }

    public List<Integer> getSampleIntList()
    {
        return sampleIntList;
    }

    public List<Integer> getSampleIntList2()
    {
        return sampleIntList2;
    }
}
