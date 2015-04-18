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

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import java.awt.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TableTraceView implements ITraceView
{
    public static String[] colNames = new String[]{ "TIMESTAMP", "SOURCE", "ID", "TARGET", "ID", "METHOD", "ELAPSED" };

    List<TraceInfo> current = new LinkedList<>();
    TraceInfoTableModel model;

    public TableTraceView(int x, int y, int width, int height)
    {
        model = new TraceInfoTableModel();

        JTable table = new JTable(model);
        table.getColumn(colNames[0]).setMinWidth(170);
        table.getColumn(colNames[1]).setMinWidth(260);
        table.getColumn(colNames[3]).setMinWidth(260);

        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Table");
            frame.setPreferredSize(new Dimension(width, height));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JScrollPane(table));
            frame.pack();
            frame.setVisible(true);
            frame.setLocation(x, y);
        });
    }

    private class TraceInfoTableModel extends AbstractTableModel
    {
        @Override
        public String getColumnName(int col)
        {
            return colNames[col];
        }

        @Override
        public int getRowCount()
        {
            return current.size();
        }

        @Override
        public int getColumnCount()
        {
            return colNames.length;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex)
        {
            TraceInfo row = current.get(rowIndex);
            if (0 == columnIndex)
            {
                return new Date(row.start);
            }
            else if (1 == columnIndex)
            {
                return row.sourceInterface;
            }
            else if (2 == columnIndex)
            {
                return row.sourceId;
            }
            else if (3 == columnIndex)
            {
                return row.targetInterface;
            }
            else if (4 == columnIndex)
            {
                return row.targetId;
            }
            else if (5 == columnIndex)
            {
                return row.methodId;
            }
            else if (6 == columnIndex)
            {
                return row.end - row.start;
            }
            return null;
        }
    }

    @Override
    public void trace(final TraceInfo info)
    {
        current.add(info);
        if (current.size() > 50)
        {
            current.remove(0);
        }
        if ((System.currentTimeMillis() - lastUpdate) > 500)
        {
            lastUpdate = System.currentTimeMillis();
            model.fireTableDataChanged();
        }
    }

    long lastUpdate = 0;


}
