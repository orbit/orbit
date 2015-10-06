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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.ldap.LdapStorageExtension;
import com.ea.orbit.actors.test.FakeClusterPeer;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "example", partitions = {
        @CreatePartition(name = "example", suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(entryLdif = "dn: dc=example,dc=com\n"
                        + "objectclass: domain\n"
                        + "objectclass: top\n"
                        + "objectclass: extensibleObject\n" + "dc: example")) })
@CreateLdapServer(
        transports =
                {
                        @CreateTransport(protocol = "LDAP", port = 10389)
                })
public class LdapPersistenceTest extends AbstractLdapTestUnit
{
    public static LdapServer ldapServer;

    private String clusterName = "cluster." + Math.random();

    @Test(timeout = 60000L)
    public void ldapTest() throws Exception
    {
        Stage stage = createStage();

        LdapAuthenticate user1 = Actor.getReference(LdapAuthenticate.class, "rmello");
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

        LdapAuthenticate user2 = Actor.getReference(LdapAuthenticate.class, "jcrawford");
        boolean authenticated2 = user2.authenticate("mypassword").join();
        assertFalse(authenticated2);
        user2.register("Johno", "Crawford", "08780290", "mypassword").join();
        authenticated2 = user2.authenticate("something").join();
        assertFalse(authenticated2);
        authenticated2 = user2.authenticate("mypassword").join();
        assertTrue(authenticated2);

        Stage stage2 = createStage();

        LdapAuthenticate user3 = Actor.getReference(LdapAuthenticate.class, "jcrawford");
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

    public Stage createStage() throws Exception
    {
        Stage stage = new Stage();
        LdapStorageExtension extension = new LdapStorageExtension();
        extension.setDn("uid=admin,ou=system");
        extension.setCredentials("secret");
        stage.addExtension(extension);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.bind();
        return stage;
    }

    @Before
    public void setUp() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection("localhost", 10389);
        connection.bind("uid=admin,ou=system", "secret");
        connection.add(new DefaultEntry("ou=people, dc=example, dc=com",
                "objectclass: organizationalUnit",
                "objectclass: top",
                "ou: people"));
        connection.close();
    }

}
