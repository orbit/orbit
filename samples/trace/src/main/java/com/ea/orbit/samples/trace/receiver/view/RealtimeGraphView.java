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
import com.ea.orbit.samples.trace.receiver.TraceReceiver;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.XChartPanel;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class RealtimeGraphView implements ITraceView
{

    private List<Long> yData = new ArrayList();
    private AtomicLong requestsPerSecond = new AtomicLong(0);

    public RealtimeGraphView(int x, int y, int width, int height)
    {
        Chart chart = new Chart(width, height);
        chart.setChartTitle("Actors Requests per Second");
        chart.setXAxisTitle("Time");
        chart.setYAxisTitle("Requests");
        chart.getStyleManager().setYAxisMin(0);
        yData.add(0L);
        chart.addSeries("requests", null, yData);
        final XChartPanel chartPanel = new XChartPanel(chart);
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Realtime");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(chartPanel);
            frame.pack();
            frame.setVisible(true);
            frame.setLocation(x, y);
        });

        TimerTask chartUpdaterTask = new TimerTask()
        {
            @Override
            public void run()
            {
                long currentValue = (long) (requestsPerSecond.incrementAndGet() * (1000.0 / (TraceReceiver.TRACE_STEP_MILLIS * 5.0)));
                yData.add(currentValue);
                requestsPerSecond.set(0);
                while (yData.size() > 100)
                {
                    yData.remove(0);
                }
                long max = yData.stream().mapToLong(l -> l).max().getAsLong();
                chart.getStyleManager().setYAxisMax(max + 10);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    chartPanel.updateSeries("requests", new ArrayList<>(yData));
                });
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(chartUpdaterTask, 0, TraceReceiver.TRACE_STEP_MILLIS * 5);
    }

    @Override
    public void trace(final TraceInfo info)
    {
        requestsPerSecond.incrementAndGet();
    }
}
