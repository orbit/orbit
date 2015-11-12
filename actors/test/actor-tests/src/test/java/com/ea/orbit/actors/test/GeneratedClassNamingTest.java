package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by daniels on 19/08/2015.
 */
public class GeneratedClassNamingTest extends ActorBaseTest
{
    public interface SomeName extends Actor {

    }

    @Test
    public void testReferenceName()
    {
        final Class<? extends SomeName> aClass = DefaultDescriptorFactory.ref(SomeName.class, "0").getClass();
        assertEquals(SomeName.class.getName() + "$Reference", aClass.getName());

    }

}
