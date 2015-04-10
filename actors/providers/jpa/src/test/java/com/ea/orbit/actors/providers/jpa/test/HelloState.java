package com.ea.orbit.actors.providers.jpa.test;

import com.ea.orbit.actors.providers.jpa.JpaState;

import javax.persistence.Entity;

@Entity
public class HelloState extends JpaState
{

    public String lastName;


}
