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

package com.ea.orbit.actors.providers.postgresql;

import com.ea.orbit.actors.providers.IStorageProvider;
import com.ea.orbit.actors.providers.json.ActorReferenceModule;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.runtime.ReferenceFactory;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.*;

public class PostgreSQLStorageProvider implements IStorageProvider {

    private String host = "localhost";
    private int port = 5432; // PostgreSQL standard port
    private String database;
    private String username;
    private String password;

    private Connection conn;
    private PreparedStatement insertState;
    private PreparedStatement updateState;
    private PreparedStatement readState;
    private PreparedStatement clearState;

    private ObjectMapper mapper;

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setDatabase(final String database) {
        this.database = database;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public synchronized Task<Void> clearState(final ActorReference<?> reference, final Object state) {
        try {
            clearState.setString(1, getName(reference));
            clearState.setString(2, getIdentity(reference));
            clearState.execute();
            return Task.done();
        } catch(SQLException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public synchronized Task<Boolean> readState(final ActorReference<?> reference, final Object state) {
        String actor = getName(reference), identity = getIdentity(reference);
        try {
            readState.setString(1, actor);
            readState.setString(2, identity);
            ResultSet results = readState.executeQuery();
            if (results.next()) {
                String json = results.getString("state");
                mapper.readerForUpdating(state).readValue(json);
                results.close();
                return Task.fromValue(true);
            } else
                return Task.fromValue(false);
        } catch(SQLException | IOException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public synchronized Task<Void> writeState(final ActorReference<?> reference, final Object state) {
        String actor = getName(reference), identity = getIdentity(reference);
        try {
            String serializedState = mapper.writeValueAsString(state);
            updateState.setString(1, serializedState);
            updateState.setString(2, actor);
            updateState.setString(3, identity);
            if (updateState.executeUpdate() < 1) {
                insertState.setString(1, actor);
                insertState.setString(2, identity);
                insertState.setString(3, serializedState);
                insertState.execute();
            }
            return Task.done();
        } catch(SQLException | JsonProcessingException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public Task<Void> start() {
        // initialize DB connection
        loadDriver();
        openConn();
        prepareStatements();
        createTableIfNotExists();

        // initialize JSON mapper
        mapper = new ObjectMapper();
        mapper.registerModule(new ActorReferenceModule(new ReferenceFactory()));
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        return Task.done();
    }

    @Override
    public Task<Void> stop() {
        try {
            this.conn.close();
            return Task.done();
        } catch (SQLException e) {
            throw new UncheckedException(e);
        }
    }

    private void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new UncheckedException(e);
        }
    }

    private void openConn() {
        try {
            this.conn = DriverManager.getConnection(toConnString(), username, password);
        } catch (SQLException e) {
            throw new UncheckedException("open connection to postgres failed", e);
        }
    }

    private void createTableIfNotExists() {
        if(!tableExists()) {
            try {
                Statement stmt = this.conn.createStatement();
                stmt.execute("CREATE TABLE actor_states ( actor text NOT NULL, identity text NOT NULL, state text NOT NULL, PRIMARY KEY (actor, identity) )");
                stmt.close();
            } catch(SQLException e) {
                throw new UncheckedException(e);
            }
        }
    }

    private boolean tableExists() {
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet results = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'actor_states')");
            boolean exists = results.next() && results.getBoolean(1);
            stmt.close();
            return exists;
        } catch(SQLException e) {
            throw new UncheckedException(e);
        }
    }

    private void prepareStatements() {
        try {
            this.insertState = this.conn.prepareCall("INSERT INTO actor_states (actor, identity, state) VALUES (?, ?, ?)");
            this.updateState = this.conn.prepareCall("UPDATE actor_states SET state = ? WHERE actor = ? AND identity = ?");
            this.readState = this.conn.prepareCall("SELECT state AS \"state\" FROM actor_states WHERE actor = ?  AND identity = ?");
            this.clearState = this.conn.prepareCall("DELETE FROM actor_states WHERE actor = ? AND identity = ?");
        } catch(SQLException e) {
            throw new UncheckedException(e);
        }
    }

    private String toConnString() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    private String getName(final ActorReference<?> reference) {
        return ActorReference.getInterfaceClass(reference).getSimpleName();
    }

    private String getIdentity(final ActorReference<?> reference) {
        return String.valueOf(ActorReference.getId(reference));
    }
}
