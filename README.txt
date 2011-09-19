** SLF4Android Readme **
------------------------

This distribution contains version 1.6.2 of slf4android, a logging
component of the interdroid platform.


What is SLF4Android?
--------------------

This library is a binding for SLF4J available from: http://www.slf4j.org/

This is a more feature rich alternative to the binding available here:
http://www.slf4j.org/android/

The intention is to make it easy to control logging from legacy code systems
properly. As such each log method checks to see if that log level is enabled
since the legacy code this adapts for is often written with out such checks
counting on the logging system to handle filtering out these messages. This
is not the best thing for performance but we view it as the lesser of two
evils.

Note that most legacy logging system use a class name as the tag. Android
however requires tags be of a specific (and short) length. As such all class
names are automatically shortened in some way. To see how the class name you
are interested in gets shortened monitor the 'slf4j' tag. Generally we try to
use the whole class name if it will fit but more often this will get
shortened to the first letters of each portion of the path followed by the
class name or simply the class name if that will not fit.

In contrast with the implementation above, we do not add * to package names
when they are shortened as we feel this is a waste of precious space in
the android log tag.

Note also that we check isDebugEnabled with every call despite this adding
possible overhead since a great deal of legacy code does not check the
logging level first.

To configure this logger you can include an SLF4J.properties file in your apk
with lines of the form:

<package>.<class>=<level>

where level is one of: 'disabled', 'trace', 'debug', 'info', 'warn' or
'error'

You may also include a 'default.log.level=<level>' line to set the default
level for all classes. Note that it is allowed to specify a class twice in
which case the lowest level specified in the file will be used except for the
default where the last instance in the file will be used.

It is also possible to ask this logger to check the android log level by
adding "android.util.Log.check=true" to your properties file.

It is also possible to have all log statements forced into a single tag
for your application using:
force.tag=true
With this option we suggest adding:
force.tag.prepend=true
Which will make all logged statements include what the log tag would have
been at the start of the log line.

We search for the properties file in the root of your JAR, in the META-INF
directory then the org/slf4j directory then org/slf4j/impl directory and stop
searching as soon as we find one.

Finally, it is possible to efficiently disable all logging entirely
by including a class named NOSLF4J in the default package in which case
we will disable all logging without checking for log properties. This
is perfect for a production environment.

Contact:
--------

More information can be found on the Interdroid project website:

  http://interdroid.net

The latest Interdoird Util source repository tree is accessible through Git at:

You can send bug reports, feature requests, cries for help, or descriptions of 
interesting way in which you have used SmartSockets to: palmer at cs.vu.nl 

Legal stuff:
------------

slf4android has been developed as part of the Interdroid project, a
software project of the Computer Systems group of the Computer
Science department of the Faculty of Sciences at the Vrije
Universiteit, Amsterdam, The Netherlands.  The main goal of the Interdroid
project is to create distributed middleware for mobile systems.

slf4android is free software. See the file "LICENSE.txt" for copying
permissions.


