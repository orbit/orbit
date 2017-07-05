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
import cloud.orbit.concurrent.ConcurrentHashSet;
import cloud.orbit.concurrent.Task;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ReminderControllerActor extends AbstractActor<ReminderControllerActor.State> implements ReminderController, ShardedReminderController
{
    public static class State
    {
        public ConcurrentHashSet<ReminderEntry> reminders = new ConcurrentHashSet<>();
    }

    private final Map<ReminderEntry, Registration> local = new ConcurrentHashMap<>();

    @Override
    public Task<String> registerOrUpdateReminder(final Remindable actor, final String reminderName, final Date startAt, final long period, final TimeUnit timeUnit)
    {
        final ReminderEntry newReminder = new ReminderEntry();
        newReminder.setPeriod(timeUnit.toMillis(period));
        newReminder.setStartAt(startAt);
        newReminder.setReminderName(reminderName);
        newReminder.setReference(actor);
        final Registration oldReminderData = local.remove(newReminder);
        if (oldReminderData != null)
        {
            oldReminderData.dispose();
        }
        registerLocalTimer(newReminder);
        // removes the previous reminder (reference,reminderName)
        state().reminders.remove(newReminder);
        // adds the new one.
        state().reminders.add(newReminder);
        // saves the state and returns the data
        return writeState().thenReturn(() -> reminderName);
    }

    private void registerLocalTimer(final ReminderEntry reminderEntry)
    {
        // adjusting start date.
        long dueTime = reminderEntry.getStartAt().getTime() - ActorRuntime.getRuntime().clock().millis();
        if (dueTime < 0)
        {
            dueTime = reminderEntry.getPeriod() + (dueTime % reminderEntry.getPeriod());
        }
        final Registration localData = registerTimer(() -> callRemainder(reminderEntry), dueTime, reminderEntry.getPeriod(), TimeUnit.MILLISECONDS);
        local.put(reminderEntry, localData);
    }

    private Task<?> callRemainder(final ReminderEntry reminderEntry)
    {
        reminderEntry.getReference().receiveReminder(reminderEntry.getReminderName(), null);
        // ignoring the return, reminders are fire and forget.
        return Task.done();
    }

    @Override
    public Task<String> unregisterReminder(final Remindable actor, final String reminderName)
    {
        final ReminderEntry newReminder = new ReminderEntry();
        newReminder.setReminderName(reminderName);
        newReminder.setReference(actor);
        final Registration oldReminderData = local.remove(newReminder);
        if (oldReminderData != null)
        {
            oldReminderData.dispose();
        }
        // removes the previous reminder (reference,reminderName)
        state().reminders.remove(newReminder);
        // saves the state and returns the data
        return writeState().thenReturn(() -> reminderName);
    }

    @Override
    public Task<List<String>> getReminders(final Remindable actor)
    {
        final List<String> list = state.reminders.stream()
                .filter(r -> reference.equals(actor))
                .map(r -> r.getReminderName())
                .collect(Collectors.toList());
        return Task.fromValue(list);
    }

    @Override
    public Task<List<String>> getReminders()
    {
        return Task.fromValue(state.reminders.stream()
                .map(ReminderEntry::getReminderName)
                .collect(Collectors.toList()));
    }

    @Override
    public Task<Void> ensureStart()
    {
        return Task.done();
    }

    @Override
    public Task<?> activateAsync()
    {
        getLogger().debug("activated");
        // registering the local timers.
        return super.activateAsync().thenRun(
                () -> state.reminders.forEach(r -> registerLocalTimer(r)));
    }

    @Override
    public Task<?> deactivateAsync()
    {
        local.values().forEach(r -> r.dispose());
        local.clear();
        return super.deactivateAsync();
    }
}
