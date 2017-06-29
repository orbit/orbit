/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.annotation.NeverDeactivate;
import cloud.orbit.concurrent.Task;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@NeverDeactivate
public interface ShardedReminderController extends Actor
{
    /**
     * Adds or updates a reminder
     *
     * @param actor        the actor that owns of the reminder
     * @param reminderName the name, used with actor as reminder key
     * @param dueTime      the first time the reminder will trigger
     * @param period       the period of the reminder after the first time.
     * @param timeUnit     the time unit for period
     * @return a task that returns the reminder name
     */
    Task<String> registerOrUpdateReminder(Remindable actor, String reminderName,
                                          Date dueTime,
                                          long period,
                                          TimeUnit timeUnit);

    /**
     * Cancels a reminder registration.
     * It's not guaranteed that the reminder won't be called after this method a the invocation might have already being triggered
     *
     * @param actor        the target actor
     * @param reminderName the reminder handle
     * @return a task holding the reminder name.
     */
    Task<String> unregisterReminder(Remindable actor, String reminderName);

    /**
     * Gets all reminders tied to the actor actor.
     *
     * @param actor the target actor
     * @return a task holding the list of reminder names.
     */
    Task<List<String>> getReminders(Remindable actor);

    /**
     * Gets all reminders.
     *
     * @return a task holding the list of reminder names.
     */
    Task<List<String>> getReminders();

    Task<Void> ensureStart();
}
