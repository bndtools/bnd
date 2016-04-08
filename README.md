# README bnd
bnd is a swiss army knife for OSGi, it creates manifest headers for you based on 
analyzing the class code, it verifies your settings, it manages project dependencies,
gives you quote of the day, diffs jars, and much more. 

The information about bnd can be found at <http://bnd.bndtools.org>

## Repository
The git repository contains all code. It contains the following projects:

* aQute.libg - Library to be statically linked (Conditional-Package)
* biz.aQute.bnd - A command line utility and ant plugin
* biz.aQute.bndlib - The core library
* biz.aQute.bndlib.tests - Tests for the core library
* biz.aQute.jpm - Just another package manager for Java
* biz.aQute.junit - Junit tester (runs on Java 1.5)
* biz.aQute.launcher - Launcher (runs on Java 1.6)
* biz.aQute.repository - Different repos with OBR
* biz.aQute.resolve - OBR Resolver
* cnf - Configuration directory
* demo - Used in testing
* dist - Contains the distribution after 'gradle'

## License
All code is Apache 2.0 Licensed so you can do what you want with the source code. 

## API
Though I am usually pretty good at backward compatibility, there is no guarantee. This
is an unpaid project and one of the most annoying thing of work is being backward compatible
on the Java API when you know a better way to do it. Properties will be backward compatible
if there is any possibility. So be aware, its ok to use this package but do not complain
if new releases require some work.

If you're building a tool with a general audience, e.g. bndtools,  that includes bnd 
for a general audience I would appreciate if you got in touch with me so I can keep 
in touch. I am always interested in ideas.

## Eclipse
bndtools is the development environment of bnd. An earlier Eclipse plugin in bnd is no longer
available.

## Building
Gradle is used to build bnd. The workspace root has a `build.gradle` file that builds all projects in proper order.

`./gradlew`              - Assembles, tests and releases the projects into `dist/bundles`  
`./gradlew :dist:build`  - Assembles and tests the projects  
`./gradlew :dist:index`  - Assembles and releases the projects into `dist/bundles`  

The workspace root includes the gradle wrapper, `gradlew`, command.

[![CloudBees Build Status](https://bndtools.ci.cloudbees.com/job/bnd.master/badge/icon)](https://bndtools.ci.cloudbees.com/job/bnd.master/)
[![Travis CI Build Status](https://travis-ci.org/bndtools/bnd.svg?branch=master)](https://travis-ci.org/bndtools/bnd)

## Release
bnd is continuously built and released on [CloudBees](https://bndtools.ci.cloudbees.com/).

[![Built on DEV@cloud](http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png)](http://www.cloudbees.com/foss/foss-dev.cb)

## Feedback
Feedback is always welcome, for general discussions use the <bndtools-users@googlegroups.com> list. We also have the <bndtools-dev@googlegroups.com> list for discussions on the development of bnd.  
One can subscribe to the mail list by sending an email to `bndtools-users+subscribe@googlegroups.com` and unsubscribe by sending an email to `bndtools-users+unsubscribe@googlegroups.com`.

Bugs and issues should go to <https://github.com/bndtools/bnd/issues>

Other feedback or specific functionality send to <Peter.Kriens@aQute.biz>

## Contributing

Want to hack on bnd? There are [instructions](CONTRIBUTING.md) to get you
started.

They are probably not perfect, please let us know if anything feels
wrong or incomplete.
