package com.ea.orbit.actors.runtime;

import java.util.Map;

public interface ActorState
{
    /**
     * @return a snapshot of the actor state data, as a Map of Key-Value pairs
     */
    Map<String, Object> asMap();

    /**
     * Updates the actor state
     *
     * @param values
     * @return
     */
    void putAll(Map<String, Object> values);

    /**
     * Sets an extended property not initially defined in the actor state.
     * <p>
     * Useful for extensions that modify the actor behaviour and need to pig back on the actor storage.
     * </p>
     *
     * @param key
     * @return the extended property value or null
     */
    Object getExtendedProperty(String key);

    /**
     * Sets an extended property not initially defined in the actor state.
     *
     * @param key
     * @param value null means remove the property
     */
    void setExtendedProperty(String key, Object value);

    /**
     * Invoke state event used for event sourced states
     *
     * @param eventName the event method name (they must be unique)
     * @param args      the event arguments
     * @return the event method return type (usually Task or void)
     */
    default Object invokeEvent(String eventName, Object[] args)
    {
        throw new IllegalArgumentException("Invalid event: " + eventName);
    }
}
