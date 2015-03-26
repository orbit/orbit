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

import com.ea.orbit.util.IOUtils;

import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AnnProcTest
{
    @Test
    public void test() throws URISyntaxException, IOException
    {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("Need a system compiler to do this test", compiler);

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        final String outputDir = "target/testing/classes";
        new File(outputDir).mkdirs();
        final String sourceDir = "target/testing/src";
        new File(sourceDir + "/annotation").mkdirs();
        new File(sourceDir + "2/annotation").mkdirs();

        copyResources("/annotation/AAA.java", sourceDir + "/annotation/AAA.java");
        copyResources("/annotation/SomeModule.java", sourceDir + "/annotation/SomeModule.java");

        final String targetPath = outputDir + "/annotation";
        final String[] outputList = {
                outputDir + "/annotation/AAA.class",
                outputDir + "/annotation/SomeModule.class",
                outputDir + "/annotation/SomeModule.moduleClasses",
        };

        for (final String fName : outputList)
        {
            File targetFile = new File(targetPath + fName);
            targetFile.delete();
        }

        List<SimpleJavaFileObject> srcFiles = new ArrayList<>();
        for (final String fName : Arrays.asList("SomeModule", "AAA"))
        {
            assertNotNull(fName, getClass().getResource("/annotation/" + fName + ".java"));
            srcFiles.add(new JavaFileObject(sourceDir + "/annotation/" + fName + ".java"));
        }

        assertTrue(compiler.getTask(null, fileManager, null, Arrays.asList("-d", outputDir), null, srcFiles).call());
        for (final String fName : outputList)
        {
            File targetFile;
            targetFile = new File(fName);
            assertTrue(targetFile + " should exist", targetFile.exists());
        }

        // happy path, full compilation
        Set<String> lines = readLines(outputDir + "/annotation/SomeModule.moduleClasses");
        assertTrue(lines.contains("annotation.AAA"));
        assertTrue(lines.contains("annotation.SomeModule"));
        assertEquals(2, lines.size());

        srcFiles = Arrays.asList(new JavaFileObject(sourceDir + "/annotation/SomeModule.java"));

        // partial compilation, just the module class
        assertTrue(compiler.getTask(null, fileManager, null, Arrays.asList("-d", outputDir), null, srcFiles).call());
        lines = readLines(outputDir + "/annotation/SomeModule.moduleClasses");
        assertEquals(2, lines.size());
        assertTrue(lines.contains("annotation.SomeModule"));
        assertTrue(lines.contains("annotation.AAA"));


        // partial compilation, just the component class (AAA)
        assertTrue(new File(outputDir + "/annotation/SomeModule.moduleClasses").delete());
        srcFiles = Arrays.asList(new JavaFileObject(sourceDir + "/annotation/AAA.java"));
        assertTrue(compiler.getTask(null, fileManager, null, Arrays.asList("-d", outputDir), null, srcFiles).call());
        lines = readLines(outputDir + "/annotation/SomeModule.moduleClasses");
        assertEquals(2, lines.size());
        assertTrue(lines.contains("annotation.SomeModule"));
        assertTrue(lines.contains("annotation.AAA"));

        assertTrue(new File(outputDir + "/annotation/AAA.class").delete());

        // partial compilation, just the module class, but the component BBB class and source have disappeared
        compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, null, null);
        copyResources("/annotation/SomeModule.java", sourceDir + "2/annotation/SomeModule.java");

        srcFiles = Arrays.asList(new JavaFileObject(sourceDir + "2/annotation/SomeModule.java"));
        assertTrue(compiler.getTask(null, fileManager, null, Arrays.asList("-d", outputDir), null, srcFiles).call());
        lines = readLines(outputDir + "/annotation/SomeModule.moduleClasses");
        assertTrue(lines.contains("annotation.SomeModule"));
        assertFalse(lines.contains("annotation.AAA"));
        assertEquals(1, lines.size());

    }

    private void copyResources(final String resource, final String destination) throws IOException
    {
        IOUtils.copy(new InputStreamReader(getClass().getResourceAsStream(resource)), new File(destination));
    }

    private Set<String> readLines(final String fileName) throws IOException
    {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName)))
        {
            return bufferedReader.lines().collect(Collectors.toSet());
        }
    }

    private class JavaFileObject extends SimpleJavaFileObject
    {
        public JavaFileObject(final String fName) throws URISyntaxException
        {
            super(new File(fName).toURI(), Kind.SOURCE);
        }

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
    }
}
