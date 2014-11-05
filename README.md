UtilityPersistenceProject
=========================
- Version: 0.x
- Author: Sebastian Lindholm
- Requires Java version 8 or higher


About
------
- Jetty with a custom session manager that optimizes session core.storage between a local and a remote session store.
- Works with Vaadin 7.1.1 using mrosin's PersistenceUIInithandler
- Main configuration in web.xml, storageConf and amazonConf files.
- Run with: mvn clean compile exec:java (requires Maven)
- Package as executable überjar: mvn clean compile package 


Bugs/Issues
------------
- Restore the correct Vaadin UI on session load


TODO
-----
- Test storage manager for Amazon S3(?)