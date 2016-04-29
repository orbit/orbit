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

package cloud.orbit.actors.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FakeClock extends Clock
{
    private AtomicLong millis = new AtomicLong(System.currentTimeMillis());
    private ZoneId zoneId = Clock.systemUTC().getZone();
    private boolean stopped;

    @Override
    public ZoneId getZone()
    {
        return zoneId;
    }

    @Override
    public Clock withZone(final ZoneId zone)
    {
        FakeClock clock = new FakeClock();
        clock.millis.set(this.millis.get());
        clock.zoneId = zone;
        return clock;
    }

    @Override
    public Instant instant()
    {
        return Instant.ofEpochMilli(millis());
    }

    @Override
    public long millis()
    {
        if (stopped)
        {
            return millis.get();
        }
        else
        {
            return System.currentTimeMillis();
        }
    }

    public long incrementTimeMillis(long offsetMillis)
    {
        return this.millis.addAndGet(offsetMillis);
    }

    public long incrementTime(long time, TimeUnit timeUnit)
    {
        return incrementTimeMillis(timeUnit.toMillis(time));
    }

    /**
     * Detaches the FakeClock from the system clock.
     * However, when stooped, every call to millis() will always increment the clock by 1 ms
     */
    public void stop()
    {
        stopped = true;
        // ensure monotonic (https://en.wikipedia.org/wiki/Monotonic_function)
        millis.set(Math.max(System.currentTimeMillis(), millis.get()));
    }
}
