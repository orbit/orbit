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

package cloud.orbit.actors.cloner;

import cloud.orbit.actors.annotation.Immutable;
import cloud.orbit.actors.runtime.Message;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public class CloneHelperTest
{

    @Test
    public void testImmutableClasses() throws Exception
    {
        assertFalse(CloneHelper.needsCloning("string"));
        assertFalse(CloneHelper.needsCloning(1337L));
        assertFalse(CloneHelper.needsCloning(new ImmutableClass("huuhaa")));
    }

    @Test
    public void testMutableClasses() throws Exception
    {
        assertTrue(CloneHelper.needsCloning(new MutableClass()));
        assertFalse(CloneHelper.needsCloning(ImmutableEnum.TEST));
        assertFalse(CloneHelper.needsCloning(null));
    }

    @Test
    public void testMessageWithObjectArrayPayload() throws Exception
    {
        assertFalse(CloneHelper.needsCloning(new Message().withPayload(new Object[]{ 1337L, "string" })));
        assertTrue(CloneHelper.needsCloning(new Message().withPayload(new Object[]{ new ImmutableClass("huuhaa"), new MutableClass() })));
        assertFalse(CloneHelper.needsCloning(new Message().withPayload(new Object[]{ new ImmutableClass("huuhaa"), new ImmutableClass("huuhaa") })));
        assertFalse(CloneHelper.needsCloning(new Message().withPayload(new Object[]{ CloneHelperTest.ImmutableEnum.TEST })));
    }

    private enum ImmutableEnum
    {
        TEST
    }

    @Immutable
    private class ImmutableClass
    {
        private final String id;

        public ImmutableClass(final String id)
        {
            this.id = id;
        }
    }

    private class MutableClass
    {
        private String id;
    }
}
