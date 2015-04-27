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

package com.ea.orbit.async.maven.plugin;

import com.ea.orbit.async.instrumentation.Transformer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


public abstract class AbstractAsyncMojo extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[]{ "**/package.html" };

    private static final String[] DEFAULT_INCLUDES = new String[]{ "**/**" };

    /**
     * List of files to include.
     * Fileset patterns relative to the input directory whose contents
     * is being instrumented.
     */
    @Parameter
    private String[] includes;

    /**
     * List of files to include.
     * Fileset patterns relative to the input directory whose contents
     * is being instrumented.
     */
    @Parameter
    private String[] excludes;

    /**
     * Directory where the instrumented files will be written.
     * If not set the files will be overwritten with the new data.
     */
    @Parameter(required = false)
    private File outputDirectory;


    /**
     * Prints each file being instrumented
     */
    @Parameter
    protected boolean verbose = false;

    /**
     * Return the specific output directory to instrument.
     */
    protected abstract File getClassesDirectory();

    protected abstract String getType();

    /**
     * Instruments the files.
     */
    public void execute() throws MojoExecutionException
    {
        if (getClassesDirectory() == null || (!getClassesDirectory().exists() || getClassesDirectory().list().length < 1))
        {
            getLog().info("Skipping instrumentation of the " + getType());
        }
        else
        {
            instrumentFiles();
        }
    }

    private String[] getIncludes()
    {
        if (includes != null && includes.length > 0)
        {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes()
    {
        if (excludes != null && excludes.length > 0)
        {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }

    private File getOutputDirectory()
    {
        if (outputDirectory == null)
        {
            return getClassesDirectory();
        }
        return outputDirectory;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Instrument the files that require instrumentation.
     */
    public void instrumentFiles() throws MojoExecutionException
    {
        try
        {
            File contentDirectory = getClassesDirectory();
            if (!contentDirectory.exists())
            {
                getLog().warn(getType() + " directory is empty! " + contentDirectory);
                return;
            }
            final Iterator<PlexusIoResource> it = getFiles(contentDirectory);
            final Transformer transformer = new Transformer();
            int instrumentedCount = 0;
            while (it.hasNext())
            {
                final PlexusIoResource resource = it.next();
                if (resource.isFile() && resource.getName().endsWith(".class"))
                {
                    byte[] bytes = null;
                    try (InputStream in = resource.getContents())
                    {
                        bytes = transformer.instrument(in);
                    }
                    catch (Exception e)
                    {
                        getLog().error("Error instrumenting " + resource.getName(), e);
                    }
                    if (bytes != null)
                    {
                        if (isVerbose())
                        {
                            getLog().info("instrumented: " + resource.getName());
                        }
                        else if (getLog().isDebugEnabled())
                        {
                            getLog().debug("instrumented: " + resource.getName());
                        }
                        IOUtil.copy(bytes, new FileOutputStream(new File(getOutputDirectory(), resource.getName())));
                        instrumentedCount++;
                    }
                }
            }
            getLog().info("Orbit Async " + getType() + " instrumented: " + instrumentedCount);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error assembling instrumenting", e);
        }
    }

    private Iterator<PlexusIoResource> getFiles(final File contentDirectory) throws IOException
    {
        if (!contentDirectory.isDirectory())
        {
            throw new ArchiverException(contentDirectory.getAbsolutePath() + " is not a directory.");
        }

        final PlexusIoFileResourceCollection collection = new PlexusIoFileResourceCollection();

        collection.setIncludes(getIncludes());
        collection.setExcludes(getExcludes());
        collection.setBaseDir(contentDirectory);
        collection.setIncludingEmptyDirectories(false);
        collection.setPrefix("");
        collection.setUsingDefaultExcludes(true);

        return collection.getResources();
    }
}
