___
___
# Testing
This section describes how bnd implements a pluggable testing framework. With most tools that use bnd this information is hidden behind a pleasant GUI (sometimes). However, in certain cases it is necessary to understand how bnd handles testing. 

## Model
In the classic way testing consists of launching a new VM, dynamically loading the classes to be tested and then running the test framework on those. As this violates module boundaries as if they did not exist there is another way. 

In bnd there is a header that allows a bundle to define what tests it contains:

  Test-Cases ::= fqn+

Such a bundle therefore exports a number of test cases to the environment. These test cases can then be executed by the tester. The default tester in bnd is the biz.aQute.junit bundle, but this can be overridden because the launcher and the tester are pluggable.

The default tester can run in ''one shot'' mode or in ''automatic'' mode. The one shot mode is used in build tools to fire a test and parse the result. In this mode, the build tool provides the class names to test.

In automatic mode, the default tester creates a list of available bundles with test cases and executes all of them. Automatic mode can then end or it can run in continuous mode. Every time a bundle is started it will run the tests again. Continuous mode is intended for developing test bundles. You just run a framework and edit your test bundle's code. Any changes are saved and deployed, triggering the tests.

## Framework Properties for the Default Tester
The default tester uses the project information to parametrize the tester's runtime component. However, it is also possible to set these runtime parameters explicitly with framework properties when you want to run the framework in automatic mode. The default tester obeys the following framework properties:

||!Property||!Default||!Description||
||`tester.port`||-||The port to send the JUnit information to in a format defined by Eclipse. If this property is not set, no information is send but tests are still run.||
||`tester.host`||`localhost`|| The host to send the JUnit information to in a formatted defined by Eclipse. If this property is not set localhost is assumed.||
||`tester.names`||||Fully qualified names of classes to test. If this property is `null` automatic mode is chosen, otherwise these classes are going to be tested and then the test ends.||
||`tester.dir`||`testdir`||A directory to put the test reports. If the directory does not exist, no reports are generated. The default is `testdir` in the default directory. This directory is not automatically created so to see the results it is necessary to ensure such a directory exists. the files in the test directory are usable in Hudson/Jenkins as test reports||
||`tester.continuous`||`false`||In automatic mode, no `tester.names` set, continue watching the bundles and re-run a bundle's tests when it is started.||
||`tester.trace`||`false`||Trace the test framework in detail. Default is false, must be set to true or false.||

## How to set the Test-Cases Macro Automatically
The Test-Cases macro can be set by hand but this can become a maintenance nightmare. A very useful macro is the `${classes}` macro. This macro inspects the JAR and can find all classes that are considered test cases:

  Test-Cases = ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}

This example looks for concrete classes that somehow extend the junit base class.

## Continuous Testing
To setup an environment to test continuously, the following launcher can be used:

  Test-Cases: ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}
  -runfw: org.apache.felix.framework
  -buildpath: osgi.core;version=[4.1','5), &#92;
	osgi.cmpn,  &#92;
	junit.osgi
  Private-Package: org.example.tests
  -runtrace: true
  -runbundles: biz.aQute.junit
  -runproperties:  &#92;
        tester.trace=true, &#92;
	tester.continuous=true, &#92;
	tester.dir=testdir

The example setup creates a bundle containing the org.example.tests package and sets the `Test-Cases` header to all testcases in that package. If you run this setup, it runs the project bundle with the biz.aQute.junit bundle. This tester bundle is parametrized with the `tester.*` properties to have trace on, test continuous, and put the test reports in `./testdir`.

You can find a bndtools project that shows this at [Github][https://github.com/bnd/aQute/tree/master/aQute.testing].

## Other Tester Frameworks
The biz.aQute.junit is a normal bundle that gets started from the launcher framework. However, before bnd chooses the default tester, it scans the classpath of the launcher (set with `-runpath`) for JARs that have the following header set:

  Tester-Plugin ::= fqn

The `Tester-Plugin` header points to a class that must extend the `aQute.bnd.build.ProjectTester` class. This class is loaded in the bnd environment and not in the target environment. This ProjectTester plugin then gets a chance to configure the launcher as it sees fit. It can get properties from the project and set these in the Project Launcher so they can be picked up in the target environment.

As this is a quite specific area the rest of the documentation is found in the source code.
