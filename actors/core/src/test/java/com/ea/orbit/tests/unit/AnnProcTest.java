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

package com.ea.orbit.tests.unit;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AnnProcTest
{
    @Test
    public void test() throws URISyntaxException
    {

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("Need a system compiler to do this test", compiler);
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        final String outputDir = "target/testing";
        new File(outputDir).mkdirs();

        final String targetPath = outputDir + "/com/ea/orbit/tests/unit/";
        final List<SimpleJavaFileObject> srcFiles = new ArrayList<>();
        final String[] outputList = {
                targetPath + "SomeActorFactory.java",
                outputDir + "/META-INF/orbit/actors/interfaces/com.ea.orbit.tests.unit.ISomeActor",
                outputDir + "/META-INF/orbit/actors/classes/com.ea.orbit.tests.unit.SomeActor"
        };

        for (final String fName : outputList)
        {
            File targetFile = new File(targetPath + fName + ".java");
            targetFile.delete();
        }

        final List<String> files = Arrays.asList("ISomeActor", "SomeActor");
        for (final String fName : files)
        {
            final File targetFile = new File(targetPath + fName + ".javadoc.properties");
            targetFile.delete();
            assertFalse(targetFile.exists());

            assertNotNull(fName, getClass().getResource(fName + ".java"));
            srcFiles.add(new SimpleJavaFileObject(
                    getClass().getResource(fName + ".java").toURI(),
                    JavaFileObject.Kind.SOURCE)
            {
                @Override
                public InputStream openInputStream() throws IOException
                {
                    return uri.toURL().openStream();
                }

                @Override
                public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException
                {
                    return IOUtils.toString(openInputStream());
                }
            });

        }
        assertTrue(compiler.getTask(null, fileManager, null, Arrays.asList("-d", outputDir), null, srcFiles).call());
        for (final String fName : outputList)
        {
            File targetFile;
            targetFile = new File(fName);
            assertTrue(targetFile + " should exist", targetFile.exists());
        }
    }
}
