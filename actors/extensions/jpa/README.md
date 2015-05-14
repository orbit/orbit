JPA Storage Provider
============

This provider materializes states in relational databases.

Requirements
=======

- The states cant be inner classes
- The states must extend JpaState.class
- The states must have the @Entity annotation
- States classes should not be shared between Actors, each one must have its own

Note: All requirements dont affect other storage extensions.

Tables
=======

Tables are automatically created, and the jpa ddl strategy at the persistence.xml defines if the tables will be updated when a new field is created.