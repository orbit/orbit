package com.ea.orbit.actors.runtime;

import com.google.common.collect.MapMaker;

import java.util.Map;

public class Agora
{
    private Map<EntryKey, Object> localObject = new MapMaker().weakValues().makeMap();
    // from implementation to reference
    private Map<Object, RemoteReference> localObjectToReference = new MapMaker().weakKeys().makeMap();

}
