A﻿Utility-Based Session Management System, integrated with Jetty 
===============================================================
- Program Author: Sebastian Lindholm, design by Benjamin Byholm, Ivan Porres
- Developed in the context of the TIVIT Digital Services Program http://www.digital-services.fi

About
-----
- Jetty with a custom session manager that optimizes session storage between a local and a remote session store.
- Works with Vaadin 7.1.1 using Martin Rosin's PersistenceUIInithandler

Installation
------------
- Requires Java version 8 or higher, Maven
- Main configuration in web.xml, storageConf and amazonConf files.
- Run with: mvn clean compile exec:java 
- Package as executable überjar: mvn clean compile package 


Issues
------
- Restore the correct Vaadin UI on session load. Addressing this issues requires changes in the Vaadin framework.


TODO
-----
- Test storage manager for Amazon S3(?)
