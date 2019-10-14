---
order: 310
title: Testing
layout: default
version: 3.0
---

This section describes how bnd implements a pluggable testing framework. With most tools that use bnd this information is hidden behind a pleasant GUI (sometimes). However, in certain cases it is necessary to understand how bnd handles testing. 

## Model
In the classic way testing consists of launching a new VM, dynamically loading the classes to be tested and then running the test framework on those. As this violates module boundaries as if they did not exist there is another way. 

In bnd there is a header that allows a bundle to define what tests it contains:

	Test-Cases ::= fqn+

Such a bundle therefore exports a number of test cases to the environment. These test cases can then be executed by the tester. The default tester in bnd is the `biz.aQute.tester` bundle, but this can be overridden because the launcher and the tester are pluggable (see [Other Tester Frameworks](#other-tester-frameworks)).

The default tester can run in **one shot** mode or in **automatic** mode. The **one shot mode** is specified using the `tester.names` property - the tester will run the specified tests and then exit. This mode is typically used by build tools, which set the `tester.names` property, run the tests, and then parse the results.

In **automatic mode**, the default tester creates a list of available bundles with the `Test-Cases` header set and executes all of them. Automatic mode will then end, or if the `tester.continuous` property is set it will continue running (known as **continuous mode**).

In **continuous mode**, every time a test bundle is started the tester will run that bundle's tests. Continuous mode is intended for developing test bundles. You just run a framework and edit your test bundle's code. Any changes are saved and deployed, triggering a restart of the bundles and hence a re-run of the tests.

## How to set the `Test-Cases` header Automatically
The `Test-Cases` header can be set by hand but this can become a maintenance nightmare. A very useful macro is the `${classes}` macro. This macro inspects the JAR and can find all classes that are considered test cases:

	Test-Cases = ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}

This example looks for concrete classes that extend the JUnit 3 base class. For non JUnit 3 tests, you can use a naming convention:

	Test-Cases: ${classes;CONCRETE;PUBLIC;NAMED;*Test*} 

This will include all concrete public classes whose name includes the word `Test`.

## Default Tester - `biz.aQute.tester`

If you do not explicitly specify the tester module to use, bnd will use `biz.aQute.tester`. The configuration of this module is as follows.

### Supported Framework Properties 

The default tester uses the project information to parameterize the tester's runtime component. However, it is also possible to set these runtime parameters explicitly with framework properties when you want to run the framework in automatic mode. The default tester obeys the following framework properties:

| Property       | Default     | Description                                                  |
|----------------|-------------|:-------------------------------------------------------------|
|`tester.port`| -           |The port to send the JUnit information to in a format defined by Eclipse. If this property is not set, no information is send but tests are still run.|
|`tester.host`|`localhost`| The host to send the JUnit information to in a formatted defined by Eclipse. If this property is not set localhost is assumed.|
|`tester.names`||Fully qualified names of classes to test. If this property is `null` automatic mode is chosen, otherwise these classes are tested and then the test exits.|
|`tester.dir`|`testdir`|A directory to put the test reports. If the directory does not exist, no reports are generated. The default is `testdir` in the default directory. This directory is not automatically created so to see the results it is necessary to ensure such a directory exists. the files in the test directory are usable in Hudson/Jenkins as test reports|
|`tester.continuous`|`false`|In automatic mode (ie, when no `tester.names` are set), continue watching the bundles and (re-)run a bundle's tests when it is started.|
|`tester.trace`|`false`|Trace the test framework in detail. Default is false, must be set to true or false.|

### Continuous Testing
To setup an environment to test continuously, the following launcher configuration can be used:

	Test-Cases: ${classes;CONCRETE;EXTENDS;junit.framework.TestCase}
	-runfw: org.apache.felix.framework
	-buildpath: osgi.core;version='[4.1,5)', \
	  osgi.cmpn,  \
	  junit.osgi
	Private-Package: org.example.tests
	-runtrace: true
	-runbundles: biz.aQute.junit
	-runproperties:  \
	  tester.trace=true, \
	  tester.continuous=true, \
	  tester.dir=testdir

The example setup creates a bundle containing the `org.example.tests` package and sets the `Test-Cases` header to all JUnit 3 test cases in that package. If you run this setup, it runs the project bundle with the `biz.aQute.junit` bundle. This tester bundle is parameterized with the `tester.*` properties to have trace on, continuous mode on, and to put the test reports in `./testdir`.

You can find a bndtools project that shows this at [Github](https://github.com/bnd/aQute/tree/master/aQute.testing).

## Testing With JUnit 5 - `biz.aQute.tester.junit-platform`

As of 4.4, bnd includes a new tester bundle `biz.aQute.tester.junit-platform` that supports JUnit 5.

As per the [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/#overview-what-is-junit-5), JUnit 5 is comprised of three modules:

1. JUnit Platform - the framework for launching test engines.
2. JUnit Jupiter - a test engine for executing the latest JUnit 5 tests.
3. JUnit Vintage - a test engine for executing JUnit 3 & 4 tests.

`biz.aQute.tester.junit-platform` leverages JUnit Platform for discovering and launching tests. This tester will:

* scan all running bundles for any JUnit Platform-compliant `TestEngine` implementations;
* scan all running bundles for any bundles with the `Test-Cases` header set, or with any classes matching the `-tester.testnames` property;
* execute all of the discovered tests for each of the discovered engines.

### Feature compatibility with `biz.aQute.tester`

At the moment, `biz.aQute.tester.junit-platform` supports most of the features of `biz.aQute.tester`, including continuous testing and XML reporting. There are a couple of notable exceptions:

* `biz.aQute.tester` uses a number of mechanisms to inject a `BundleContext` into a running test case. Due to significant architectural differences between JUnit Platform and JUnit 3/4, this feature is not yet supported by `biz.aQute.tester.junit-platform` and may never be fully supported (or at least, not in a way that is 100% backward compatible). However, it is not difficult to work around this by manually fetching the bundle context in your tests using `FrameworkUtil` (eg, in a `@Before` method).
* The legacy XML reporting output will not include the stdout/stderr output from the tests unless you are running against a version of `junit-platform-launcher` >= 1.4.0.

### Improved Eclipse integration

In addition to JUnit Platform support, the new tester has some features that weren't available in the old tester, which makes its integration with Eclipse a bit more user-friendly: 

* Test failures are reported blue, and errors as a red x.  
* For assertions that support it, the visual diff for failed comparisons is available.
* Double-clicking on a test result will take you to the source code of the test.
* Tests are hierarchically grouped by bundle. Fragments are grouped under their host bundle.
* Handles the following JUnit features that were previously not supported:
    * Aborted tests (assumption failures)
    * Ignored/skipped tests
    * Custom display names (including full Unicode support)
    * Parameterized tests
* Better continuous mode support. With `biz.aQute.tester`, when running in continuous mode only the results of the first test run are displayed in Eclipse. With `biz.aQute.junit-platform`, if you select the *Display JUnit results in IDE every time the tester reruns tests* property in the launch configuration, then it will display the results afresh for every test run. This allows you to combine the power of continuous testing with the convenience of Eclipse's JUnit GUI.

### Using `biz.aQute.tester.junit-platform`

* Set `-tester: biz.aQute.tester.junit-platform` in your bnd file (see [Other Tester Frameworks](#other-tester-frameworks)).
* Ensure that `biz.aQute.tester.junit-platform`'s dependencies are installed in your `-runbundles`.
* Ensure that the test engines you need (and their dependencies) are also installed in your `-runbundles`.

Bnd can help with the last two steps by adding the tester and engine bundles to `-runrequires` and using the resolver:

	-runrequires: \
		bnd.identity;id='org.junit.jupiter.engine',\
		bnd.identity;id='org.junit.vintage.engine',\
		bnd.identity;id='biz.aQute.tester.junit-platform'

See the [chapter on resolving](http://bnd.bndtools.org/chapters/250-resolving.html) for more information.

Note that if you're only using JUnit 3/4, you can omit the `-runrequires` line for the Jupiter engine, and conversely if you're only using JUnit Jupiter you can omit the Vintage engine. Alternatively/additionally, if you have any other `TestEngine` implementation bundles available, you can list these here instead/as well (though this has not been tested). 

#### Finding the JUnit Platform bundles

As noted above, `biz.aQute.tester.junit-platform` requires JUnit Platform (and its dependencies) on the classpath, and if it is to do much that is useful it will also require at least one `TestEngine`. Bundled versions of these are part of Eclipse since Oxygen. You can include them in your workspace from:

* Eclipse's Orbit repository
* Bnd project's Eclipse mirror: https://dl.bintray.com/bndtools/eclipse-repo/4.7.3a:
```
    -plugin.repository: \
        aQute.bnd.repository.osgi.OSGiRepository;\
            name="Eclipse Oxygen 4.7.3a";\
            locations="https://dl.bintray.com/bndtools/eclipse-repo/4.7.3a/index.xml.gz";\
            poll.time=-1;\
            cache="${workspace}/cnf/cache/stable/EclipseOxygen"
```
* Your local Eclipse installation (using the P2Repository plugin):
```
    -plugin.repository: \
        aQute.bnd.repository.p2.provider.P2Repository;\
            name="Eclipse Local";\
            url="file:///path/to/eclipse/";\
            location="${workspace}/cnf/cache/stable/EclipseLocal"
```
Alternatively, it is not difficult to download the required (non-OSGi) modules from Maven Central and include them as-is on `-runpath`, or else (preferably) wrap them into bundles and include them in `-runrequires`/`-runbundles`.

## Other Tester Frameworks
The biz.aQute.tester is a normal bundle that gets started from the launcher framework. However, before bnd chooses the default tester, it scans the classpath for a tester (set with `-runpath`) for JARs that have the following header set:

	Tester-Plugin ::= fqn

If no such tester is found on the `-runpath` it will look in the `-tester` instruction and loads that bundle: 

	-tester:				biz.aQute.junit

Otherwise it will use `biz.aQute.tester` (if it still can find it).

The `Tester-Plugin` header points to a class that must extend the `aQute.bnd.build.ProjectTester` class. This class is loaded in the bnd environment and not in the target environment. This ProjectTester plugin then gets a chance to configure the launcher as it sees fit. It can get properties from the project and set these in the Project Launcher so they can be picked up in the target environment.

As this is a quite specific area the rest of the documentation is found in the source code.

## Older Versions

For a long time bnd had `biz.aQute.junit` as the default tester. `biz.aQute.junit` has the same functionality as `biz.aQute.tester`, but with the following key differences:

* `biz.aQute.junit` embedded the JUnit 3/4 classes and exported them. `biz.aQute.tester` imports the JUnit classes like any other bundle, giving you flexibility in which version you wish to use.
* `biz.aQute.junit` added itself to the `-runpath` and then executed the tests from there, making itself (and JUnit) part of the system bundle. In contrast, `biz.aQute.tester` adds itself to `-runbundles`.

Unfortunately the design of `biz.aQute.junit` caused constraints between JUnit and bnd that was not good because JUnit itself is not directly a shining example of software engineering. :-( So for this reason, `biz.aQute.tester` (or the newer `biz.aQute.tester.junit-platform`) is generally preferred.

If for some reason you need to be backward compatible with the older model, set:

	-tester: biz.aQute.junit
