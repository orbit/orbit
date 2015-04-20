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

package com.ea.orbit.samples.chat;


import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@ServerEndpoint("/sample/chat/{chatName}")
public class ChatWebSocket
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChatWebSocket.class);
    private IChat chat;
    private IChatObserver observer;

    @OnOpen
    public void onWebSocketConnect(Session session)
    {
        chat = IActor.getReference(IChat.class, session.getPathParameters().get("chatName"));
        observer = new IChatObserver()
        {
            @Override
            public Task<Void> receiveMessage(final ChatMessageDto message)
            {
                JsonObject jsonObject = Json.createObjectBuilder()
                        .add("message", message.getMessage())
                        .add("sender", message.getSender())
                        .add("received", ZonedDateTime.ofInstant(message.getWhen().toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT))
                        .build();

                session.getAsyncRemote().sendObject(jsonObject.toString());
                return Task.done();
            }
        };
        chat.join(observer);

        chat.getHistory(100).thenAccept(ms -> {
                    JsonArrayBuilder array = Json.createArrayBuilder();
                    ms.stream().forEach(
                            m -> array.add(
                                    Json.createObjectBuilder()
                                            .add("message", m.getMessage())
                                            .add("sender", m.getSender())
                                            .add("received", m.getWhen().toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT))
                                            .build()
                            )
                    );
                    session.getAsyncRemote().sendObject(
                            Json.createObjectBuilder().add("history", array).build().toString());
                }
        );

        logger.info("Socket Connected: " + session);
    }

    @OnMessage
    public void onWebSocketText(String jsonMessage, Session session)
    {
        JsonObject jsonObject = Json.createReader(new StringReader(jsonMessage)).readObject();

        ChatMessageDto message = new ChatMessageDto();
        message.setSender(jsonObject.getString("sender"));
        message.setMessage(jsonObject.getString("message"));

        logger.info("Received TEXT message: " + message);
        chat.say(message);
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason)
    {
        logger.info("Socket Closed: " + reason);
        chat.leave(observer);
    }

    @OnError
    public void onWebSocketError(Throwable cause)
    {
        logger.error("websocket reported an error", cause);
    }
}