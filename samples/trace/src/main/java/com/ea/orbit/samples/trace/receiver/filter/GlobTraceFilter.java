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

package com.ea.orbit.samples.trace.receiver.filter;

import com.ea.orbit.samples.trace.messaging.TraceInfo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlobTraceFilter implements ITraceFilter
{
    private JTextField field = new JTextField();
    private Matcher matcher;
    private Pattern pattern;

    public GlobTraceFilter(int x, int y, int width, int height)
    {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Glob Filter (Target)");
            frame.setPreferredSize(new Dimension(width, height));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            field.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(final DocumentEvent e)
                {
                    changedUpdate(e);
                }

                @Override
                public void removeUpdate(final DocumentEvent e)
                {
                    changedUpdate(e);
                }

                public void changedUpdate(DocumentEvent e)
                {
                    String current = field.getText();
                    if (current == null)
                    {
                        pattern = null;
                        return;
                    }
                    String regex = GlobUtil.convertGlobToRegEx(current);
                    pattern = Pattern.compile(regex);
                    matcher = pattern.matcher("");
                }

            });

            frame.add(field);
            frame.pack();
            frame.setLocation(x, y);
            frame.setVisible(true);
        });
    }

    @Override
    public boolean allows(final TraceInfo info)
    {
        if (pattern != null)
        {
            return matcher.reset(info.targetInterface).find();
        }

        return true;
    }
}
