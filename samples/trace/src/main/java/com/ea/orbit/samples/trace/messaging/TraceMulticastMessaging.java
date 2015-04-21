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

package com.ea.orbit.samples.trace.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TraceMulticastMessaging implements ITraceMessaging
{
    private String groupAddress = "239.0.0.3";
    private int port = 8888;
    private Thread receiverThread;
    private Thread senderThread;
    private ObjectMapper mapper = new ObjectMapper();
    private ConcurrentLinkedQueue<TraceInfo> receiveQueue = new ConcurrentLinkedQueue<>();
    private LinkedBlockingQueue<TraceInfo> sendQueue = new LinkedBlockingQueue<>();
    private byte[] readBuffer = new byte[65535];

    public TraceMulticastMessaging()
    {
    }

    public void start()
    {

        receiverThread = new Thread(() ->
        {
            try
            {
                InetAddress address = InetAddress.getByName(groupAddress);
                MulticastSocket clientSocket = new MulticastSocket(port);
                clientSocket.joinGroup(address);
                while (true)
                {
                    DatagramPacket msgPacket = new DatagramPacket(readBuffer, readBuffer.length);
                    clientSocket.receive(msgPacket);
                    TraceInfo info = mapper.readValue(readBuffer, 0, readBuffer.length, TraceInfo.class);
                    receiveQueue.offer(info);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
        senderThread = new Thread(() ->
        {
            try
            {
                InetAddress group = InetAddress.getByName(groupAddress);
                while (senderThread.isAlive())
                {
                    TraceInfo info = sendQueue.take();
                    try
                    {
                        String out = mapper.writeValueAsString(info);
                        byte[] buf = out.getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
                        DatagramSocket socket = new DatagramSocket();
                        socket.send(packet);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public void stop()
    {
        receiverThread.interrupt();
        senderThread.interrupt();
    }

    @Override
    public void send(final TraceInfo info)
    {
        sendQueue.offer(info);
    }

    @Override
    public TraceInfo receive()
    {
        return receiveQueue.poll();
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public void setGroupAddress(final String groupAddress)
    {
        this.groupAddress = groupAddress;
    }

    public String getGroupAddress()
    {
        return groupAddress;
    }

    public int getPort()
    {
        return port;
    }
}
