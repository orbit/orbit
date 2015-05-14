package com.ea.orbit.actors.extensions.jpa;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class JpaState
{
    @Id
    protected String stateId;
}
