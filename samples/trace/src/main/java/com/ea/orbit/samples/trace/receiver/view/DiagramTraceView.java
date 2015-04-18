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

package com.ea.orbit.samples.trace.receiver.view;

import com.ea.orbit.samples.trace.messaging.TraceInfo;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DiagramTraceView implements ITraceView
{
    private Graph graph;

    public DiagramTraceView(String cssFile)
    {
        System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        graph = new MultiGraph("Cluster", true, true);
        graph.addAttribute("ui.stylesheet", "url('file:" + cssFile + "')");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        graph.display();
    }

    ConcurrentLinkedQueue<String> edgeKeys = new ConcurrentLinkedQueue<>();

    public void trace(final TraceInfo info)
    {
        try
        {
            String sourceName = info.sourceInterface.substring(info.sourceInterface.lastIndexOf('.') + 1) + info.sourceId;
            String targetName = info.targetInterface.substring(info.targetInterface.lastIndexOf('.') + 1) + info.targetId;
            getNodeOrCreate(sourceName);
            getNodeOrCreate(targetName);

            String edgekey = sourceName + "_" + targetName;
            Edge edge;
            if (!edgeKeys.contains(edgekey))
            {
                edge = graph.addEdge(edgekey, sourceName, targetName, true);
                edgeKeys.offer(edgekey);
            }
            else
            {
                edge = graph.getEdge(edgekey);
            }

            if (edge != null)
            {
                graph.getEdge(edgekey).setAttribute("ui.style", "fill-color:rgb(" + ((int) (Math.random() * 255)) + "," + ((int) (Math.random() * 255)) + "," + ((int) (Math.random() * 255)) + ");");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private Node getNodeOrCreate(String name)
    {
        Node node = graph.getNode(name);
        if (node == null)
        {
            node = graph.addNode(name);
            node.setAttribute("ui.label", name);
        }
        return node;
    }

}
