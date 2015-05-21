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

package com.ea.orbit.container.test;

import com.ea.orbit.container.Container;
import com.ea.orbit.container.test.module.test.Class1;
import com.ea.orbit.container.test.module.test.Class2;
import com.ea.orbit.container.test.module.test.Module1;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class ModuleTest
{
    private static final Logger logger = LoggerFactory.getLogger(ModuleTest.class);

    @Test
    public void testModule()
    {
        final Module1 module = new Module1();
        assertNotNull(module.getClasses());
        assertEquals(2, module.getClasses().size());
        assertTrue(module.getClasses().contains(Class1.class));
        assertTrue(module.getClasses().contains(Class2.class));
    }

    @Test
    public void testModuleWithContainer()
    {
        final Module1 module = new Module1();

        final Container container = new Container();
        container.add(Module1.class);
        container.start();
        final Class2 c2 = container.get(Class2.class);
        assertTrue(c2.started);
        assertSame(c2, container.get(Class2.class));
        assertNotSame(container.get(Class1.class), container.get(Class1.class));
    }

}
