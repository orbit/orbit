/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.profiler;

import cloud.orbit.io.StringBuilderWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;


public class ProfileDump
{
    private ProfileDump()
    {

    }

    public static String textMethodInfo(ProfilerData data, long globalCount)
    {
        StringBuilderWriter sw = new StringBuilderWriter();
        PrintWriter pw = new PrintWriter(sw);
        float limit = 5;
        // obs: can't have it changing during the dump
        ArrayList<MethodInfo> methods = new ArrayList<>(data.getMethods());

        // this might go wrong if the data is changing meanwhile.
        long sampleCount = data.getSampleCount();
        Collections.sort(methods, (a, b) -> -Long.compare(a.count, b.count));
        for (MethodInfo m : methods)
        {
            if (sampleCount != 0 && limit > (m.count * 100.0 / sampleCount))
            {
                break;
            }
            if (globalCount != 0)
            {
                pw.print(String.format("%.2f", m.count * 100.0 / globalCount));
                pw.print("\t");
            }
            if (sampleCount != 0)
            {
                pw.print(String.format("%.2f", m.count * 100.0 / sampleCount));
                pw.print("\t");
            }
            pw.print(m.count);
            pw.print("\t");
            pw.print(m.declaringClass);
            pw.print(".");
            pw.print(m.methodName);
            pw.println();
        }
        return sw.toString();
    }

    public static String textDumpCallTree(CallTreeElement root, long globalCount)
    {
        StringBuilderWriter sw = new StringBuilderWriter();
        PrintWriter pw = new PrintWriter(sw);

        textDumpCallTree(64, pw, "", root, root.count, globalCount);

        return sw.toString();
    }

    public static void textDumpCallTree(int depthLimit, PrintWriter pw, String prefix, CallTreeElement start, long sampleCount, long globalCount)
    {
        float limit = 5;
        ArrayList<CallTreeElement> methods = new ArrayList<>(start.children.values());
        Collections.sort(methods, (c1, c2) -> -Long.compare(c1.count, c2.count));
        for (int i = 0; i < methods.size(); i++)
        {
            CallTreeElement m = methods.get(i);
            float percTotalGlobal = m.count * 100f / globalCount;
            float percTotal = m.count * 100f / sampleCount;
            if (percTotal > limit)
            {
                float percMethod = m.elementInfo.count * 100f / sampleCount;
                pw.print(String.format("%6.2f%%", percTotalGlobal));
                pw.print("\t");
                pw.print(String.format("%6.2f%%", percTotal));
                pw.print("\t");
                pw.print(String.format("%6.2f%%", percMethod));
                pw.print("\t");
                pw.print(prefix);
                pw.print("+- ");
                pw.print(m.elementInfo.methodName);
                pw.print("\t");
                pw.print(m.elementInfo.declaringClass);
                pw.println();
                boolean hasMore = i + 1 < methods.size();
                if (hasMore)
                {
                    CallTreeElement m2 = methods.get(i + 1);
                    float percTotal2 = m2.count * 100f / sampleCount;
                    hasMore = percTotal2 > limit;
                }
                if (depthLimit > 0)
                {
                    textDumpCallTree(--depthLimit, pw, hasMore ? (prefix + "|  ") : prefix + "   ", m, sampleCount, globalCount);
                }
            }
        }
    }


}
