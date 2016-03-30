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

import cloud.orbit.exception.UncheckedException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassDescriptor<T>
{
    private Class<T> clazz;
    private Map<String, FieldDescriptor> declaredFieldsMap;
    private volatile List<FieldDescriptor> declaredFields;
    private volatile ClassDescriptor<?> superClass;
    private ClassCache cache;
    private List<FieldDescriptor> allInstanceFields;

    public ClassDescriptor(ClassCache cache, Class<T> clazz)
    {
        this.cache = cache;
        init(clazz);
    }

    private void init(final Class<T> clazz)
    {
        this.setClazz(clazz);

        declaredFields = createFieldDescriptors();
        declaredFieldsMap = new LinkedHashMap<>();
        for (FieldDescriptor fieldDescriptor : declaredFields)
        {
            declaredFieldsMap.put(fieldDescriptor.getName(), fieldDescriptor);
        }

        if (clazz == Object.class)
        {
            allInstanceFields = Collections.emptyList();
        }
        else
        {
            final List<FieldDescriptor> parentFields = getSuperClass().getAllInstanceFields();
            final List<FieldDescriptor> declaredFields = getDeclaredFields();
            final ArrayList<FieldDescriptor> fields = new ArrayList<>(parentFields.size() + getDeclaredFields().size());
            fields.addAll(parentFields);
            for (FieldDescriptor field : declaredFields)
            {
                int mod = field.getField().getModifiers();
                if (!Modifier.isStatic(mod))
                {
                    fields.add(field);
                }
            }
            this.allInstanceFields = Collections.unmodifiableList(fields);
        }
    }

    protected List<FieldDescriptor> createFieldDescriptors()
    {
        final Field[] clazzDeclaredFields = clazz.getDeclaredFields();
        final List<FieldDescriptor> declaredFields = new ArrayList<>(clazzDeclaredFields.length);
        for (Field f : clazzDeclaredFields)
        {
            FieldDescriptor fieldDescriptor = createFieldDescriptor(f);
            declaredFields.add(fieldDescriptor);
        }
        return Collections.unmodifiableList(declaredFields);
    }

    protected FieldDescriptor createFieldDescriptor(Field f)
    {
        return new FieldDescriptor(f);
    }

    public ClassDescriptor<?> getSuperClass()
    {
        return (superClass != null) ? superClass
                : (superClass = cache.getClass(getClazz().getSuperclass()));
    }

    public T newInstance()
    {
        try
        {
            return getClazz().newInstance();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }


    public Class<T> getClazz()
    {
        return clazz;
    }

    public void setClazz(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    public Map<String, FieldDescriptor> getDeclaredFieldsMap()
    {
        return declaredFieldsMap;
    }

    public List<FieldDescriptor> getDeclaredFields()
    {
        return declaredFields;
    }

    public List<FieldDescriptor> getAllInstanceFields()
    {
        return allInstanceFields;
    }
}
