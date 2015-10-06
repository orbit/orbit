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

package com.ea.orbit.actors.extensions.ldap;

import com.ea.orbit.actors.extensions.AbstractStorageExtension;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LdapStorageExtension extends AbstractStorageExtension
{

    private String dn;
    private String credentials;
    private String host = "localhost";
    private int port = 10389;

    @Override
    public Task<Void> start()
    {
        return Task.done();
    }

    @Override
    public Task<Void> stop()
    {
        return Task.done();
    }

    @Override
    public Task<Void> clearState(final ActorReference reference, final Object state)
    {
        LdapConnection connection = null;
        try
        {
            connection = acquireConnection();
            connection.delete(absoluteDn(reference, entity(state)));
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            if (connection != null)
            {
                releaseConnection(connection);
            }
        }
        return Task.done();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Boolean> readState(final ActorReference reference, final Object state)
    {
        LdapConnection connection = null;
        try
        {
            LdapEntity entity = entity(state);
            connection = acquireConnection();
            EntryCursor cursor = connection.search(absoluteDn(reference, entity), "(objectclass=*)", SearchScope.OBJECT, "*");
            if (cursor.next())
            {
                Map<String, Field> map = getFieldAttributeMap(state.getClass());
                for (String key : map.keySet())
                {
                    map.get(key).set(state, cursor.get().get(key).get().getString());
                }
            }
            else
            {
                return Task.fromValue(false);
            }
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
        finally
        {
            if (connection != null)
            {
                releaseConnection(connection);
            }
        }
        return Task.fromValue(true);
    }

    private Map<String, Field> getFieldAttributeMap(Class clazz)
    {
        //TODO this is cacheable
        Map<String, Field> map = new HashMap<>();

        List<Field> fields = new ArrayList<>();
        for (Class c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            final Field[] declaredFields = c.getDeclaredFields();
            if (declaredFields != null && declaredFields.length > 0)
            {
                for (Field f : declaredFields)
                {
                    fields.add(f);
                }
            }
        }
        fields.stream().filter(f -> Modifier.isPublic(f.getModifiers()) || f.isAnnotationPresent(LdapAttribute.class)).forEach(f -> {
            LdapAttribute attrann = f.getAnnotation(LdapAttribute.class);
            if (attrann == null)
            {
                map.put(f.getName(), f);
            }
            else
            {
                map.put(attrann.value() == null ? f.getName() : attrann.value(), f);
            }
        });
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Void> writeState(final ActorReference reference, final Object state)
    {
        LdapConnection connection = null;
        try
        {
            LdapEntity entity = entity(state);

            List<String> attributes = new ArrayList(Arrays.asList(entity.attributes()));
            Map<String, Field> map = getFieldAttributeMap(state.getClass());
            for (String key : map.keySet())
            {
                attributes.add(key + ": " + map.get(key).get(state).toString());
            }

            connection = acquireConnection();
            EntryCursor cursor = connection.search(absoluteDn(reference, entity), "(objectclass=*)", SearchScope.OBJECT, "*");
            if (cursor.next())
            {

                List<Modification> modifications = new ArrayList<>();
                for (String tmp : map.keySet())
                {
                    String value = (String) map.get(tmp).get(state);
                    Modification mod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, tmp, value);
                    modifications.add(mod);
                }
                connection.modify(cursor.get().getDn(), modifications.toArray(new Modification[0]));

            }
            else
            {
                connection.add(new DefaultEntry(absoluteDn(reference, entity), attributes.toArray()));
            }
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
        finally
        {
            if (connection != null)
            {
                releaseConnection(connection);
            }
        }
        return Task.done();
    }

    private LdapConnection acquireConnection() throws LdapException, IOException
    {
        //TODO acquire from pool (exists at the ldap client api, change later)
        LdapConnection connection = new LdapNetworkConnection(getHost(), getPort());
        if (getCredentials() == null)
        {
            connection.bind(getDn());
        }
        else
        {
            connection.bind(getDn(), getCredentials());
        }
        return connection;
    }

    private void releaseConnection(LdapConnection connection)
    {
        //TODO release to pool (exists at the ldap client api, change later)
        try
        {
            connection.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private LdapEntity entity(Object state)
    {
        return state.getClass().getAnnotation(LdapEntity.class);
    }

    private String relativeDn(ActorReference reference, LdapEntity entity)
    {
        return entity.dnKey() + "=" + getIdentity(reference);
    }

    private String absoluteDn(ActorReference reference, LdapEntity entity)
    {
        return relativeDn(reference, entity) + ", " + entity.baseDn();
    }

    public String getDn()
    {
        return dn;
    }

    public void setDn(final String dn)
    {
        this.dn = dn;
    }

    public String getCredentials()
    {
        return credentials;
    }

    public void setCredentials(final String credentials)
    {
        this.credentials = credentials;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }


}
