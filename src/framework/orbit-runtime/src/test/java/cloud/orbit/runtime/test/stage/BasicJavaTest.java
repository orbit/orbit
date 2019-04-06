/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage;

import cloud.orbit.core.actor.AbstractActor;
import cloud.orbit.core.actor.ActorWithNoKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicJavaTest extends BaseStageTest {
    public interface JavaEchoActor extends ActorWithNoKey {
        CompletableFuture<String> echo(String msg);
    }

    @SuppressWarnings("unused")
    public static class JavaEchoActorImpl extends AbstractActor implements JavaEchoActor {
        @Override
        public CompletableFuture<String> echo(String msg) {
            return CompletableFuture.completedFuture(msg);
        }
    }

    @Test
    void basicJavaEchoTest() {
        final String msg = "X-301";
        final JavaEchoActor actor = getStage().getActorProxyFactory().createProxy(JavaEchoActor.class);
        final String result = actor.echo(msg).join();
        assertThat(result).isEqualTo(msg);
    }
}
