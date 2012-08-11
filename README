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
* dist - Contains the distribution after 'ant dist'
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
The workspace root has a build.xml that builds all project in proper order. Due
to the fact that bnd builds itself there are certain cases where you get an error
during build. Trying again should fix the issue.

Each project directory has a build.xml. The following targets are available:

ant build (default) - Build
ant clean           - Clean the project
ant test            - Run a bnd OSGi test
ant junit           - Run standard JUnit tests in the test package
ant dist            - Create a dist directory with all the bundles in repo format + obr indexes

Outputs are stored in the tmp directory in the different projects.

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

