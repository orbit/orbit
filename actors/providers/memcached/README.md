Orbit Actors Storage Provider Memcached
============

Here be dragons, the Memcached persistence test invokes "flush_all". Running it against a shared Memcache instance will result in data loss.
