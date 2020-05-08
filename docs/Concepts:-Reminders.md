# Overview
Reminders are low frequency persisted timers. They are called by the framework and may cause the activation of their actor.

The reminders are persisted and usually called as remote messages from the cluster nodes.

Reminders should not be used for high frequency tasks. 

# Using Reminders

```java
public static interface Match extends Actor, Remindable
{
    // ...
}
public class MatchActor extends AbstractActor implements Match {
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
```

# Reminder/Timer Comparison
| Timer                                      | Reminder                                                             |
|--------------------------------------------|----------------------------------------------------------------------|
| Exists only during the activation          | Exist until explicitly cancelled by the application                  |
| Can be high frequency, seconds and minutes | Should be low frequency, minutes, hours or days                      |
| Actor deactivation cancels the timer       | Reminder remains after deactivation.                                 |
| Receive a callback                         | Call a fixed method                                                  |
| Any actor can use timers                   | Only actors whose interface implements Remindable can use reminders |