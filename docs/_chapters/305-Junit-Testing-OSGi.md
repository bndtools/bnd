---
order: 305
title: Plain JUnit Testing with OSGi (PRELIMENARY)
layout: default
version: 4.2.0
---

This chapter describes how to test using the bnd _JUnit Framework_ in OSGi. This JUnit Framework is specific for the 
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

To be able to support the remote workspace it is required to add the following line to the `cnf/build.bnd` file:

    -remoteworkspace = true 

This setting enables the workspace to be available over a remote procedure call interface.

To create a test, follow the normal JUnit rules for a test project. In general means that you write your domain code in 
the `src` folder (which can be for example `src/main/java`) and then the test code in the `test` folder (which can for
example be code in `src/test/java` if you follow the maven layout.)

Any domain dependencies are put in the `-buildpath` and any test only dependencies on the `-testpath`. For example:

    -buildpath: \
        slf4j.api
    
    -testpath: \
        sl4j.simple, \
        biz.aQute.bnd.remote.junit

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

The first thing to do is to create a _JUnit Framework Builder_. This object creates the initial link to the remote workspace
where the test code was launched from, analyzes the test code, and then builds an initial _run specification_. 

A Run Specification contains all the information an OSGi framework needs to operate:  

* Framework Properties
* System packages (extra)
* System capabilities (extra)
* JARs on the classpath of the OSGi Framework 
* The OSGi Framework
* Bundles to install

To construct the Run Specification, the JUnit Framework Builder will contact the corresponding workspace to access
all repositories, resolver, and other features of this workspace. This is setup in such a way that all facilities of the
enclosing workspace of the test project are available.

The builder provides a number of methods to setup this Run Specification

* Use a bnd or bndrun file. In this case the `-run*` instructions are used to setup the OSGi Framework. This feature makes it
possible to use the resolver to find a proper set of run bundles.
* Directly adding bundles and JARs. Different methods are provided to add bundles to the `-runpath` and `-runbundles`.
* Prefab bundles. For example, it is possible to add all gogo bundles.

There are a (growing) number of methods on the builder to make it easy to setup the parameters of an OSGi framework. 
All these methods are documented with Java code. To look at the details, consult this Java code.

It is also possible to control the start of the framework. In certain cases it is important that a framework is initialized 
but not yet started. This makes it possible to, for example, hide services and replace them with a proxy. 

After the Run Specification is setup it is possible to create a new JUnit Framework instance. This class wraps around
an OSGi Framework and controls it. Such a framework is created with the `create()` method. This method can be called
multiple times for different frameworks. The builder is also still active after creating a framework and can be used
to further parameterize the to be created framework.

## The JUnit Framework

Normally when a JUnit Framework is created from the JUnit Framework Builder it is _started_. (A framework can also be created
without being started, see `nostart()`). This means that any run bundles are installed and started or an exception would
have been thrown.

The JUnit Framework has a large number of methods to control the running framework.

### Bundle Context

It is often necessary to have a Bundle Context to work with the OSGi Framework. It is convenient to have access to the Bundle
Context of the OSGi Framework but unfortunately there are a number of use cases where the framework's Bundle Context
acts subtly different. For this reason, the builder automatically adds a synthetic _test_ bundle. Although this bundle does
not contain any code, it represents the test code. This bundle has the project name but then in upper case.

Calling the `getBundleContext()` method will generally return the Bundle Context of the synthetic test bundle since this is the context 
that will usually be used by the actual code when the application runs. However, if the builder is set to `notestbundle()`
then the `getBundleContext()` method will then fall back to the Bundle Context of the framework. 

### Lifecycle Management Bundles

A number of methods are provided to add more bundles to the running framework. This can go via a bnd/bndrun file, a File
object, or a run specification. These bundles are installed but not yet started. Bundles can also be stopped and
started.

For example, the following shows how to install a number of bundles from the repository:

        framework.bundles("com.example.foo;version=3.0, foobar;version=file")
        
### Working with Services

Convenience methods are provided on the JUnit Framework to get services and register services. Special methods are there
to wait some time until a service is registered.

        FooService foo = framework.waitForService( FooService.class, 50000 );
        
### Injection of objects

Fetching and waiting for a number of services or other framework related variables is boilerplate code. The JUnit Framework
therefore contains a mini-injection engine that can inject:

* Services (ServiceReference, Maps, services, etc.)
* Bundles
* Bundle Context

Injection services will delay until all services are present.

Injection points must be marked with the `@Service` annotation. This annotation provides the following fields:

* `timeout` – Specify the time to wait for the service to arrive. If the value is 0, the default, then a custom amount of time will be waited. 
* `target` – A target filter. 
* `service` – The type of the service to inject. This is used when the type of the target point (method, field) is not 
  sufficient to find the service in the service registry. This can happen because the type is a Map or the use of inheritance.

The annotation can be applied to the following types, where T is the service type:

* T
* ServiceReference<T>
* Optional<T>
* Optional<ServiceReference<T>>
* Collection<T>
* Collection<ServiceReference<T>>

The injection can take place on fields (preferred) and on methods. Methods can specify multiple injection points 
with their arguments. The actual service type, if not specified by the @Service annotation is retrieved from the
first argument. 
 
For example:

        JUnitFramework          framework = builder.runfw("org.apache.felix.framework").create().inject(this);
     
        @Service
        Foobar  foobar;
        
        @Service
        ConfigurationAdmin cadmin;

        @Service(timeout=30000)
        void foobar(Foobar foobar, ServiceReference<Foobar> sr) {
            // ...
        }        

Injection is not limited to the test instance. Any object can be injected at any time. 

### Building bundles

In a lot of tests it is necessary to add a bundle to the running framework. The JUnit Framework provides a 
_Bundle Builder_. This is a builder that builds a _Builder Specification_. This specification will be send to 
the remote workspoce where it is constructed in the context of the corresponding project.  

The Bundle Builder is highly geared to create test bundles. It is, for example, trivial to create a bundle with 
a DS component:

        @Component 
        public static class MyComponent {} 


        @Test
        public void test() {
            Bundle b = framework.bundle().addResource( MyComponent.class ).start();
        }

### Proxying

A common use case in testing OSGi application is to hide a service in a complex application and replace it with a special mock
for testing.   This is supported by the `hide(Class<?> type)` method. In general, to use this method it is best to start
the framework in the `notestbundle()` mode so that no bundles are started. Calling the `hide()` method will then
make sure that all services that are going to be registered by any bundle but the test bundle are going to be hidden for
everybody else. Registering a replacement via the Bundle Context of the test bundle will then be visible to all other bundles.

For example:

        @Test
        public void test() {
            JUnitFramework framework = builder
                .nostart()
                .runfw("org.apache.felix.framework")
                .create();
            
            framework.hide( Foobar.class );
            framework.start();
            Foobar mock = mock(Foobar.class);
            framework.register(mock); 
            ...
        }



