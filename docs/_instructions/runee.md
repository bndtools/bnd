---
layout: default
class: Builder
title: -runee EE   
summary:  Define the  runtime Execution Environment capabilities, default Java 6.
---
The `-runee` instruction adds the capabilities of an _execution environment_ to the system capabilities. Every Java edition has a set of standard packages and OSGi has also defined a number of execution environments that define which packages can be found. The `-runee` allows these capabilities to be defined by using the name of the execution environment. Additionally, this instruction also adds an `osgi.ee` requirement with the given name and version. You can use the following execution environment names:

	OSGi/Minimum-1.0
	OSGi/Minimum-1.1 
	OSGi/Minimum-1.2
	JRE-1.1
	J2SE-1.2
	J2SE-1.3
	J2SE-1.4
	J2SE-1.5
	JavaSE-1.6
	JavaSE-1.7
	JavaSE/compact1-1.8
	JavaSE/compact2-1.8
	JavaSE/compact3-1.8
	JavaSE-1.8
	JavaSE-9

An example:

	-runee: JavaSE-1.8

