---
layout : page
title : "Orbit : Actor Concept - Reminders"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-observers.html"
previous: "orbit-actor-concept-timers.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-Reminders-Overview)
-  [Using Reminders](#ActorConcept-Reminders-UsingReminders)
-  [Differences between timers and reminders](#ActorConcept-Reminders-Differencesbetweentimersandreminders)



Overview {#ActorConcept-Reminders-Overview}
----------


Reminders are low frequency persisted timers. They are called by the framework and sometimes may cause the activation of their actor.


The reminders are persisted and usually called as remote messages from the cluster nodes.


Reminders should not be used for high frequency tasks. 


 


Using Reminders {#ActorConcept-Reminders-UsingReminders}
----------

**Reminder** 
{% highlight java %}
public static interface IMatch extends IActor, IRemindable
{
    // ...
}
public class Match extends OrbitActor implements IMatch {
    private long lastEvent;
 
    @Override
    public Task startMatch()
    {
        lastEvent = System.currentTimeMillis();
        timer = registerReminder("matchTimeout", 10, 10, TimeUnit.MINUTES);
        return Task.done();
    }
 
    @Override
    public Task processEvent(Match event)
    {
        lastEvent = new Date();
        // ...
        return Task.done();
    }
 
    @Override
    public Task<?> receiveReminder(String reminderName, TickStatus status)
    {
        if(System.currentTimeMillis() - lastEvent > TimeUnit.MINUTES.toMillis(15)) 
        {
            unregisterReminder("matchTimeout");
            return processMathTimeout();
        }
        return Task.done();
    }
}
{% endhighlight %}

 


Differences between timers and reminders {#ActorConcept-Reminders-Differencesbetweentimersandreminders}
----------


| Timer | Reminder |
|-------|----------|
| Exists only during the activation | Exist until explicitly cancelled by the application |
| Can be high frequency, seconds and minutes | Should be low frequency, minutes, hours or days |
| Actor deactivation cancels the timer | Reminder remains after deactivation. |
| Receive a callback | Call a fixed method |
| Any actor can use timers | Only actors whose interface implements IRemindable can use reminders |

