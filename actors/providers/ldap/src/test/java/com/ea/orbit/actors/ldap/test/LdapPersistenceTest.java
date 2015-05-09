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

package com.ea.orbit.actors.ldap.test;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.providers.ldap.LdapStorageProvider;
import com.ea.orbit.actors.test.FakeClusterPeer;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LdapPersistenceTest
{
    private String clusterName = "cluster." + Math.random();

    @Test
    public void mytest() throws Exception
    {
        OrbitStage stage = createStage();

        ILdapAuthenticateActor user1 = IActor.getReference(ILdapAuthenticateActor.class, "rmello");
        boolean authenticated1 = user1.authenticate("123456").join();
        assertFalse(authenticated1);
        user1.register("Ricardo", "Mello", "88034101", "123456").join();
        authenticated1 = user1.authenticate("654321").join();
        assertFalse(authenticated1);
        authenticated1 = user1.authenticate("123456").join();
        assertTrue(authenticated1);

        user1.register("Ricardo", "Mello", "88034101", "changedpassword").join();
        authenticated1 = user1.authenticate("123456").join();
        assertFalse(authenticated1);
        authenticated1 = user1.authenticate("changedpassword").join();
        assertTrue(authenticated1);

        ILdapAuthenticateActor user2 = IActor.getReference(ILdapAuthenticateActor.class, "jcrawford");
        boolean authenticated2 = user2.authenticate("mypassword").join();
        assertFalse(authenticated2);
        user2.register("Johno", "Crawford", "08780290", "mypassword").join();
        authenticated2 = user2.authenticate("something").join();
        assertFalse(authenticated2);
        authenticated2 = user2.authenticate("mypassword").join();
        assertTrue(authenticated2);

        OrbitStage stage2 = createStage();

        ILdapAuthenticateActor user3 = IActor.getReference(ILdapAuthenticateActor.class, "jcrawford");
        boolean authenticated3 = user3.authenticate("mypassword").join();
        assertTrue(authenticated3);

        user1.remove().join();
        authenticated1 = user1.authenticate("changedpassword").join();
        assertFalse(authenticated1);
        authenticated2 = user2.authenticate("mypassword").join();
        assertTrue(authenticated2);
        user2.remove().join();
        authenticated2 = user2.authenticate("mypassword").join();
        assertFalse(authenticated2);

        authenticated3 = user3.authenticate("mypassword").join();
        assertFalse(authenticated3);
    }

    public OrbitStage createStage() throws Exception
    {
        OrbitStage stage = new OrbitStage();
        LdapStorageProvider provider = new LdapStorageProvider();
        provider.setDn("uid=admin,ou=system");
        provider.setCredentials("secret");
        stage.addProvider(provider);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.bind();
        return stage;
    }

    @Before
    public void setup() throws Exception
    {
        removeAll();
    }

    private void removeAll() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection("localhost", 10389);
        connection.bind("uid=admin,ou=system", "secret");
        EntryCursor cursor = connection.search("ou=people, dc=example, dc=com", "(objectclass=*)", SearchScope.ONELEVEL, "*");
        while (cursor.next())
        {
            connection.delete(cursor.get().getDn());
        }
        connection.close();
    }

}
