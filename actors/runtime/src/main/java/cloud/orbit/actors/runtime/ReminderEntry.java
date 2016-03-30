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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.Remindable;

import java.io.Serializable;
import java.util.Date;

public class ReminderEntry implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Remindable reference;
    private String reminderName;

    private Date startAt;
    private long period;

     public Remindable getReference()
    {
        return reference;
    }

    public void setReference(final Remindable reference)
    {
        this.reference = reference;
    }

    public String getReminderName()
    {
        return reminderName;
    }

    public void setReminderName(final String reminderName)
    {
        this.reminderName = reminderName;
    }

    public Date getStartAt()
    {
        return new Date(startAt.getTime());
    }

    public void setStartAt(final Date startAt)
    {
        this.startAt = new Date(startAt.getTime());
    }

    public long getPeriod()
    {
        return period;
    }

    public void setPeriod(final long period)
    {
        this.period = period;
    }

    @Override
    public boolean equals(final Object o)
    {
        ReminderEntry that;
        return (this == o) || (
                (o instanceof ReminderEntry)
                        && reference.equals((that = (ReminderEntry) o).reference)
                        && reminderName.equals(that.reminderName));
    }

    @Override
    public int hashCode()
    {
        int result = reference.hashCode();
        result = 31 * result + reminderName.hashCode();
        return result;
    }

    public String toString()
    {
        return String.format("ReminderEntry[reference=%s,reminderName=%s,period=%d]", reference, reminderName, period);
    }

}