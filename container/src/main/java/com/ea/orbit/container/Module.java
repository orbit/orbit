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

package com.ea.orbit.container;

import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ClassPath;
import javax.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class Module
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Module.class);

    public List<Class<?>> getClasses()
    {
        final InputStream res = getClass().getResourceAsStream(getClass().getSimpleName() + ".moduleClasses");
        final Class<?> moduleClass = getClass();
        if (res != null)
        {
            try
            {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(res, "UTF-8")))
                {
                    return br.lines().filter(n -> n.length() > 0).map(n -> {
                        try
                        {
                            return moduleClass.getClassLoader().loadClass(n);
                        }
                        catch (ClassNotFoundException e)
                        {
                            logger.error("Error loading class: " + n, e);
                            throw new UncheckedException(e);
                        }
                    }).filter(c -> c != moduleClass).collect(Collectors.toList());
                }
            }
            catch (IOException e)
            {
                logger.error("Error on loading classes", e);
                throw new UncheckedException(e);
            }
        }
        else
        {
            String packageName = ClassPath.getNullSafePackageName(getClass()).replace('.', '/');
            return ClassPath.get().getAllResources().stream()
                    .filter(r -> r.getResourceName().startsWith(packageName))
                    .filter(r -> r.getResourceName().endsWith(".class"))
                    .map(r -> r.getResourceName())
                    .map(n -> n.substring(0, n.length() - 6).replace('/', '.'))
                    .filter(n -> !n.equals(getClass().getName()))
                    .map(n -> {
                        try
                        {
                            return Class.forName(n);
                        }
                        catch (Exception ex)
                        {
                            throw new UncheckedException(ex);
                        }
                    }).collect(Collectors.toList());
        }
    }
}
