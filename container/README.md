Orbit Container
===============

Orbit container defines a unified way to start/stop/configure orbit applications. It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com).

Application components can be loaded and configured through the definition of modules.

Internally it uses a minimal inversion of control ([IoC](http://en.wikipedia.org/wiki/Inversion_of_control)) container
to wire application objects together.

It aims to solve:

 * managing the application life cycle
 * injecting configuration
 * wiring objects through dependency injection

Documentation
=======

Documentation is located [here](http://orbit.bioware.com/).

License
=======
Orbit is licensed under the [BSD 3-Clause License](../LICENSE).