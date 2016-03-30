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

package cloud.orbit.actors;

import cloud.orbit.actors.runtime.TickStatus;
import cloud.orbit.concurrent.Task;

/**
 * Actors that register reminders must implement the interface Remindable.
 * <p>
 * Reminders are a low frequency persisted timers that will be called even if the actor is not currently activated.
 * </p>
 * <p><b>Differences between timers and reminders:</b>
 * <table summary="Differences between timers and reminders">
 * <thead>
 * <tr>
 * <th>Timer</th>
 * <th>Reminder</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>Exists only during the activation</td>
 * <td>Exist until explicitly cancelled by the application</td>
 * </tr>
 * <tr>
 * <td>Can be high frequency, seconds and minutes</td>
 * <td>Should be low frequency, minutes, hours or days</td>
 * </tr>
 * <tr>
 * <td>Actor deactivation cancels the timer</td>
 * <td>Reminder remains after deactivation.</td>
 * </tr>
 * <tr>
 * <td>Receive a callback</td>
 * <td>Call a fixed method</td>
 * </tr>
 * <tr>
 * <td>Any actor can use timers</td>
 * <td>Only actors whose interface implements IRemindable can use reminders</td>
 * </tr>
 * </tbody>
 * </table></p>
 *
 */
public interface Remindable extends Actor
{
    /**
     * Receive reminder will be called by the reminder subsystem.
     *
     * @param reminderName the name of the registered reminder
     * @param status       the current status of the reminder that triggered this call.
     * @return an application task that might be completed asynchronously.
     */
    Task<?> receiveReminder(String reminderName, TickStatus status);
}
