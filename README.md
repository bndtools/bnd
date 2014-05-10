# README bnd
bnd is a swiss army knife for OSGi, it creates manifest headers for you based on 
analyzing the class code, it verifies your settings, it manages project dependencies,
gives you quote of the day, diffs jars, and much more. 

The information about bnd can be found at http://www.aQute.biz/Bnd

## Repository
The git repository contains all code. It contains the following projects:

* aQute.libg - Library to be statically linked (Conditional-Package)
* biz.aQute.bnd - A command line utility and ant plugin
* biz.aQute.bndlib - The core library
* biz.aQute.bndlib.tests - Tests for the core library
* biz.aQute.jpm - Just another package manager for Java
* biz.aQute.junit - Junit tester (runs on Java 1.4)
* biz.aQute.launcher - Launcher (runs on Java 1.4)
* biz.aQute.repository - Different repos with OBR
* biz.aQute.resolve - OBR Resolver
* cnf - Configuration directory
* demo - Used in testing
* dist - Contains the distribution after 'gradle'
* osgi.r5 - OSGi jars without all dependencies

## License
All code is Apache 2.0 Licensed so you can do what you want with the source code. 

## API
though I am usually pretty good at backward compatibility, there is no guarantee. This
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

`gradle`              - Assembles, tests and releases the projects into dist/bundles  
`gradle :dist:build`  - Assembles and tests the projects  
`gradle :dist:index`  - Assembles and releases the projects into dist/bundles  

The workspace root also includes the gradle wrapper, `gradlew`, command if you do not have gradle installed
on your system.

## Testing
The main tests are in the biz.aQute.bndlib.tests project. These are standard JUnit tests. They
are all in the src/test directory. Tests are quite extensive and run from the
Eclipse JUnit screen.

## Release
bnd is continuously built on Cloudbees: https://bndtools.ci.cloudbees.com/#

A more comprehensive release process is in the works.

## Feedback
Feedback is always welcome, for general discussions use bndtools-users@googlegroups.com

Bugs and issues should go to https://github.com/bndtools/bnd

Other feedback or specific functionality send to Peter.Kriens@aQute.biz

