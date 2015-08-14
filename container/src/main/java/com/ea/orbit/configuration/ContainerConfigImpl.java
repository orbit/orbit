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

package com.ea.orbit.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class ContainerConfigImpl extends Properties implements ContainerConfig
{
    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, Object> getPrefixedValues(final String collectionPrefix)
    {
        final String actualPrefix = collectionPrefix + ".";

        final Map<String, Object> res = new LinkedHashMap<>();
        for (final Entry<String, Object> e : getAll().entrySet())
        {
            if (e.getKey().startsWith(actualPrefix))
            {
                res.put(e.getKey(), e.getValue());
            }
        }
        return res;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<String> getCollection(final String collectionName)
    {
        List outList = null;
        final Integer count = this.getAsInt(collectionName + ".count");

        if (count != null)
        {
            outList = new ArrayList<String>();
            for (int i = 0; i < count; ++i)
            {
                final String nextValue = this.getAsString(collectionName + "." + i);
                if (nextValue != null)
                {
                    outList.add(nextValue);
                }
            }
        }

        return outList;

    }

    @Override
    public String getAsString(final String key)
    {
        final Object o = this.get(key);
        return (o != null) ? o.toString() : null;
    }

    @Override
    public String getAsString(final String key, final String defaultValue)
    {
        String value = getAsString(key);
        if (null == value)
        {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public Integer getAsInt(final String key)
    {
        Integer value = null;
        final String string = this.getAsString(key);
        if (string != null)
        {
            value = Integer.valueOf(string);
        }
        return value;
    }

    @Override
    public Integer getAsInt(final String key, final Integer defaultValue)
    {
        Integer value = getAsInt(key);
        if (null == value)
        {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public Long getAsLong(final String key)
    {
        Long value = null;
        final String string = this.getAsString(key);
        if (string != null)
        {
            value = Long.valueOf(string);
        }
        return value;
    }

    @Override
    public Long getAsLong(final String key, final Long defaultValue)
    {
        Long value = getAsLong(key);
        if (null == value)
        {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public Boolean getAsBoolean(final String key)
    {
        Boolean value = null;
        final String string = this.getAsString(key);
        if (string != null)
        {
            value = Boolean.valueOf(string);
        }
        return value;
    }

    @Override
    public Boolean getAsBoolean(final String key, final Boolean defaultValue)
    {
        Boolean value = getAsBoolean(key);
        if (null == value)
        {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public void put(final String key, final String value)
    {
        super.put(key, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void putAll(final ContainerConfig other)
    {
        super.putAll((Map) other);
    }

    @Override
    public Map<String, Object> getAll()
    {
        final Map<String, Object> result = new HashMap<>();
        for (final Entry<Object, Object> entry : entrySet())
        {
            result.put(entry.getKey().toString(), entry.getValue());
        }

        return result;
    }
}
