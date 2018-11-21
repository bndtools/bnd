---
order: 305
title: Pure JUnit Testing with OSGi (PRELIMENARY)
layout: default
version: 4.2
---

This chapter describes how to test using the bnd _JUnit Framework_ in OSGi. This JUnit Frqmework is specific for the 
workspace mode of bndtools. This workspace mode is supported by Eclipse, Intellij, Gradle, and the standalone bnd command.
It is not supported by Maven for now.

## Background

Standard JUnit testing uses the `-buildpath` and `testpath` instructions to create a class path for a newly launched VM. However,
in an OSGi world we need more. The original test model for the OSGi was driven by the needs to provide a compliance test
that could easily be adapted to different frameworks and bundle implementations. It therefore required the testing code
to be wrapped in bundles. A bnd test framework then setup an OSGi framework based on a _run_ specification in a bnd(run) file.

For test writers this had a number of drawbacks. First, encapsulating test code in bundles was not always easy and always
an extra step. Over time it also became  cumbersome for the bnd maintainers because it required a special launcher,
JUnit code, and launching interfaces in build tools. Although the maintenance wasn't too bad, it created a lot
of moving parts and sometimes made it hard to pick up the latest and greatest in testing innovations. 

In the same time, OPS4J group developed Pax Exam. Pax Exam used the standard JUnit environment and could therefore run
in all build tools unmodified. However, Pax Exam is heavily coupled to Maven which made it not straightforward to use in
a bndtools environment where the classpath is not defined by Maven.

Therefore, there was a bit of Pax Exam envy. A couple of years ago the JUnitFramework was added as the bnd alternative
to Pax Exam with some extra features. It is based on the idea that the JUnit test creates and manages an OSGi framework
that can be manipulated by the standard off the shelf JUnit code.

A crucial problem in testing like this is managing classes that appear in the test code and that are used inside the
OSGi framework. For example, if you use an interface class `HelloWorld` in a JUnit test and export a service that
implements this interface from a bundle then it is necessary to ensure that the `HelloWorld` class is exported as
a system package by the OSGi framework. To simplify this, the bnd JUnit Framework analyzes the setup of a bnd project
and calculates the content of the test code and its imports. All these packages are then exported by the
OSGi framework from the classpath. This greatly signifies testing.

Although this worked quite well except that it added a large number of classes to the `-testpath` of the project under test for
it needed access to virtually all bndlib and repository code. Although the fact that this made testing bnd code a lot harder is not that
relevant for others, it also unfortunately includes some OSGi classes that often clashed with the projects that
were tested because they were not of the proper version.

It was therefore decided to change the strategy and remove the bndlib dependency completely. For this reason, the
Workspace was extended with an RPC interface. When the JUnit code starts up, it contacts this remote API and 
performs all analysis and bundle building in the remote process. The remote process is either Eclipse, Gradle, or another
_driver_.

To test, it is then only necessary to put a small JAR that creates the remote link and handles a number of runtime
functions.

## Quick Start

In this example we write a simple test case that test a Bundle Activator.

Basically follow the normal JUnit rules for a test project. In general means that you write your domain code in 
the `src` folder (which can be for example `src/main/java`) and then the test code in the `test` folder (which can for
example be code in `src/test/java` if you follow the maven layout.)

Any domain dependencies are put in the `-buildpath` and any test only dependencies on the `-testpath`. For example:

    -buildpath: \
        slf4j.api
    
    -testpath: \
        sl4j.simple

You can now write a simple test class in the `test` folder. Let's call it `HelloTest`.

    public class HelloTest {
    
There is a special JUnit Framework Builder class that is used to build a framework. In this case we only need a framework
and no special bundles. Neither the framework nor the bundles that are installed are required 

        JUnitFrameworkBuilder   builder     = new JUnitFrameworkBuilder();
        JUnitFramework          framework = builder.runfw("org.apache.felix.framework").create();

We define a Bundle Activator is a nested class. This nested class must be created by the OSGi framework as 
Bundle Activator. It must therefore be a `public static` class. The JUnit Framework will ensure that this
class is available on the inside of the OSGi framework as the same class as that we use in the JUnit test code.

We use a semaphore to check if the start and stop methods are actually called.

        public static class Hello implements BundleActivator {
            static Semaphore semaphore = new Semaphore(0);
            
            @Override
            public void start(BundleContext context) throws Exception {
                System.out.println("Hello");
                semaphore.release();
            }
    
            @Override
            public void stop(BundleContext context) throws Exception {
                System.out.println("Goodbye");
                semaphore.release();
            }
        }

What is left is the test method. We create a bundle on the fly with the activator and start it. We then verify that the
start method is called but not yet the stop method. We then stop the bundle and verify that the semaphore is released
again.

        @Test
        public void testActivator() throws BundleException {
            Bundle hello = framework.bundle()
                .bundleActivator(Hello.class)
                .start();
    
            assertTrue(Hello.semaphore.tryAcquire());
            assertFalse(Hello.semaphore.tryAcquire());
    
            hello.stop();
            assertTrue(Hello.semaphore.tryAcquire());
    
        }
    }
      
## Features

The following sections detail the features of the JUnit Framework

### Normal JUnit Code (not even a Runner)

There are no requirements on JUnit or other test frameworks. Although not tested, it should be possible to use TestNG. The
reason is that this is not implemented as a JUnit Runner but only uses normal code. 

## Installing Bundles

### Injection

## Bndrun files

### Building Bundles

### Components
