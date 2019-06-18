---
order: 320
title: Testing with Launchpad
layout: default
class : Project
version: 4.3.0
---

_note_ This featue is in beta. Feedback welcome and expect a few deficiencies in documentation and usage.

An OSGi framework poses special challenges to testing because it is necessary to start a framework instance for each 
test. There has always been a bnd OSGi testing framework that was developed to test the OSGi specifications and  reference
implementations. This testing framework packaged the tests as a bundle and had a special version of JUnit that could run
these tests from inside the framework. Actually quite powerful and it was fully integrated with  Eclipse JUnit testing,
delivering identical output to the CI build tools. However, the use of a special JUnit runner excluded it for people that wanted
to use TestNG or other test frameworks.

A second problem was that since tests ran inside the framework as a bundle they could not influence the setup of the OSGi
framework easily. A last problem was that tests shared the same framework which could result in ordering dependencies. 

## Launchpad

Launchpad is a bnd runtime library that provides an API to launch a framework that is fully integrated with the 
bnd workspace. It automatically exports the runtime class path via the framework bundle, ensuring there is a single class space for
the code on the class path and the code in the bundles. (This does require that bundles properly import their 
exported packages.)

Launchpad provides a builder that incrementally can build up the specifications of the framework. The builder can
take `bndrun` files or bundle specifications in the same format as that are used to set the `-buildpath` or
`-runbundles` in the `bnd.bnd` files. Once the information is setup, bnd will calculate the setup based on 
the classes in the `test` folder and launch an OSGi framework.

Once the framework is launched, Launchpad can then inject services and some key framework objects into annotated
fields. Each field can specify a timeout, target filter, and minimum cardinality. Injection can take place in any object but is
usually on the test instance.

In the original OSGi testing support test bundles had to be created during the build. In the OSGi test cases for the Blueprint
reference implementation more than 200 test bundles were used. Although the overhead was relatively small due to the bnd `-make` facility,
it was still a nuisance because the information in a test case had to be synchronized in a `bnd` file in another directory. For
this reason Launchpad contains a _bundle builder_. This bundle builder used `bnd` under the hood. It can use anything that a bnd
sub bundle could use; it is build in the context of the project that contains the test classes and shares the same 
`-buildpath`. Bundles that are build with the bundle builder can actually leverage nested classes in the test class for
Bundle Activator or component classes.

Overall this is a comprehensive library for testing OSGi projects in a Bndtools workspace.

## Quick Start

You need the `biz.aQute.launchpad` library on your `-testpath`.

	bnd.bnd:
	...	
	-buildpath: ...
	-testpath: \
		osgi.enroute.junit.wrapper, \
		biz.aQute.launchpad

_note_ The biz.aQute.launchpad is available in 4.2 but there are a few minor API changes that did not make it. You can
therefore use the snapshot version on JFrog or download the latest version and use:

    lib/biz.aQute.launchpad.jar;version=file

The next step is to enable your _workspace_ for launchpad. A bnd workspace can have a Remote Workspace Server and
Launchpad needs it. You therefore need to add the following to your `cnf/build.bnd` file.

    cnf/build.bnd:
        -remoteworkspace        true

Using JUnit, we can now create a test. We start with creating a `LaunchpadBuilder`. This builder stores information about the to be started framework. It has many
methods that usually align with the properties for a `bnd.bnd` file. The Javadoc contains the details. We could create this
object in a `@Before` method and close it in an `@After` method but this object does not have to be closed. It contains only
the settings.

	LaunchpadBuilder	builder	= new LaunchpadBuilder()
						.runfw("org.apache.felix.framework");

For this first quick start test we inject a Bundle Context, the core OSGi object that allows us to interact with the framework.

	@Service
	BundleContext		context;

The method is a normal JUnit test method. We open the Launchpad in a _try resource_ block. Closing the `Launchpad` object will
shutdown the framework. The test is simply verifying that the injection has worked.

	@Test
	public void quickStart() throws Exception {
		try (Launchpad launchpad = builder.create()
			.inject(this)) {
			assertNotNull(context);
		}
	}

Voila! The first Launchpad test case.

## Two Models

The Launchpad code can be used in different modes:

* Using the class path. In this mode classes on the classpath are exported as framework classes and override 
  any classes in the framework. This mode does not require any support from the test framework but there are
  a number of caveats. This setup works very well if you want to tests POJOs that require an OSGi context.
* JUnit Runner. In this mode bnd creates a bundle that is the project's bundle but includes the test classes. These
  test classes use DynamicImport-Package to minimize disruptions to the manifest. The Launchpad Runner will then start
  the framework, install the test bundle, and run the tests. This mode works well when you want to test your finished bundle.


## JUnit Launchpad Runner Mode

To use the Launchpad Runner it is necessary to add an `@RunWith` annotation on your JUnit test class:

    @RunWith(LaunchpadRunner.class)
    public class TestMyCode {

        LaunchpadBuilder    builder = new LaunchpadBuilder().runfw("org.apache.felix.framework").debug();

        @Service
        Launchpad       launchpad;

        @Test
        public void testMyCode() {
            launchpad.report();
        }
    }

### Junit Theory

The Launchpad Runner is in control to gather the tests and then execute them. The test gathering is handled via the
standard JUnit support. However, when the test must run, LaunchpadRunner creates a bundle that has the following qualities:

* Fully contains the project's build artifact. This is the sole bundle for a single bundle project or the first bundle (sorted 
by name) if a multi bundle project is used. The code actually uses a prior build JAR.
* All the test classes are added to this JAR
* A DynamicImport-Package * is added 

The runner then launches a framework based on a LaunchpadBuilder that it finds in the the `static` field `builder`.

It then installs the bundles, and adds the test bundle. To execute a test, it loads the
class from the test bundle and finds the appropriate method, instantiates the class in an instance, runs the injector
on this object. and executes the methods.

This mode is similar to the PAX Exam model. It has similar constraints. It does run the `@Before` and `@After` annotated
methods but it cannot run the `@BeforeClass` and `@AfterClass`.

## Classpath Mode

Launchpad is quite awesome to use but there are some pitfalls to take into account. It is strongly recommended
to read this section to get an idea how Launchpad handles class sharing between the test classes (which are on the
normal Java class path) and the classes in bundles. There is more going on than what one suspects looking at the
simplicity how it can be used. This section details the workings to make you aware of potential bugs and should 
help in debugging problems. In general, the cleaner your code base, the better this all works. If you have a very messy
setup with lots of scripts, fragments, require bundle, and very wide code interfaces instead of services then this
might not be for you ...

### Class Loading

In Java, class are loaded from the _classpath_. The class path is a (usually very long) list of Jar files. When a class
needs to be loaded, Java searches al those Jars for that class, first one wins.

In OSGi, this model is changed for a _delegating_ model. Each bundle _imports_ a set of _packages_ and _exports_ a set 
of packages. This information is in a bundle's _manifest_. When an OSGi framework _resolves_ a bundle, it wires these
imports to a corresponding export. 

### Lauchpad's Classpath

When a test case gets started the _driver_ (Eclipse, Gradle, etc.) launches a new Java VM. The class path for that
VM will consist of all entries on the `-buildpath`, `-testpath`, and the main and test output folders. When the
Launchpad builder is first called it will contact the _Remote Workspace_ in the driver and request for an analysis of 
the test code. The Remote Workspace then uses the project setup to calculate a bundle that would export all the
test code and its imports. The Launchpad Builder then makes the OSGi framework export all packages that that
virtual test bundle would have exported. That is, any class visible from the test cases will be exported by 
the OSGi framework by default. It is possible to _exclude exports_ using glob expressions or predicates from a test case.
See the `excludeExport()` methods.

### Bundles

A bundle installed on the OSGi Framework should this see all the relevant classes from the class path instead of
from other bundles. The tricky case is when a bundle exports a package that is also available from the 
class path. If this package is only exported then the framework cannot substitute it for the package from the 
class path. In such a bad case the bundle that exports it will see its embedded version of the class while
the rest of the system sees the version from the class path. This can then result in a class cast exception
like:

	java.lang.ClassCastException: an instance of org.example.Foo cannot be assigned to org.example.Foo

Although the names of those classes are identical, the problem is that they will be loaded by different class
loaders. 

### Version Sensitivity

Due to this setup there is very strict sensitivity to the version of the OSGi Framework packages and bundles. Most bnd projects
have the OSGi framework packages on the `-buildpath`. The version of these packages can be lower than the version
of the Framework because of backward compatibility. Actually, bnd generally recommends to compile against the
lowest possible framework packages. However, with launchpad these packages will also be used by the Framework, there
is unfortunately no good way around this. The consequence is that the `-buildpath` version of the Framework
packages must match the exact version used by the implementation of the framework.

### Excluding Exported System Packages

Launchpad will calculate the set of packages that are exported by the framework from the claspath. The so called
`org.osgi.framework.system.packages.extra`. It calculates this by creating bundle from the test sources, adding
all dependencies, and then exporting the full content. That export statement is then uses for `org.osgi.framework.system.packages.extra`.

However, this is generally too wide since it includes all dependencies, not just public dependencies. Version mismatches
can create nasty problems and sometimes the solution is to exclude exports. 

The Launchpad Builder provides a number of methods called `excludeExport()` that take either a _glob_ or a predicate.
The globs/predicates are then ran against the list of calculated export package names. Any matching entry is then
not exported.

    Launchpad b = new LaunchpadBuilder()
        .excludeExports( "slf4j.*")
        .create();

If a bndrun file is the `-excludeexports` instruction can be placed in the bndrun file containing a list globs.

    -excludeexports     aQute.lib*, slf4j.*

## Naming

The Launchpad has a default name of the method and class that called `create()`. These names can be overwritten with
`create(name)` and `create(name,className)`. The actual name of Launchpad is set under the following framework 
property names:

    launchpad.name
    launchpad.className

## Interaction with Services

Clearly the best part of Launchpad is that you can actually use real services and do not have
to mock them up. Many a test seems to mostly test their mocks. 

With Launchpad, real framework is running. You can inject services or register services. 

In the following example we register a service `Foo` and then verify if we can get it. We then
unregister the service and see that it no longer exists.

	interface Foo {}

	@Test
	public void services() throws Exception {
		try (Launchpad launchpad = builder.create()) {

			ServiceRegistration<Foo> register = 
				launchpad.register(Foo.class, new Foo() {});
			Optional<Foo> s = 
				launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isTrue();

			register.unregister();

			s = launchpad.waitForService(Foo.class, 100);
			assertThat(s.isPresent()).isFalse();
		}
	}

The `waitForService` methods take a timeout in milliseconds. Their purpose is to provide some leeway
during startup for the system to settle. If a service should be there then it the `getService()` methods
can be used. 

## Injection

Injection is not automatic because in many cases you want to handle the setup of the framework before
you inject. Injection can also happen as often as you want. However, you first need to create and
start the framework.

	@Test
	public void inject() throws Exception {
		try (Launchpad launchpad = builder.create()) {
			ServiceRegistration<Foo> register = launchpad.register(Foo.class, new Foo() {});

			class I {
				@Service
				Foo foo;
				@Service
				Bundle			bundles[];
				@Service
				BundleContext	context;
			}
			I inject = new I();
			launchpad.inject(inject);
			assertThat(inject.bundles).isNotEmpty();
		}
	}

## Hello World

Although Bundle-Activator's are not recommended to be used (they are singletons), they are very useful in
test cases. With Launchpad it is not necessary to to make a separate bundle, we can make a bundle with
an inner class as activator.

We therefore first define the Bundle Activator as a static public inner class of the test class:

	public static class Activator implements BundleActivator {
		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("Hello World");
		}
		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("Goodbye World");
		}
	}

The Launchpad class contains a special _Bundle builder_. This bundle builder is based on bnd and can can do everything that
bnd can do in a `bnd.bnd` file. In this case we add the Bundle Activator and start it.

	@Test
	public void activator() throws Exception {
		try (Launchpad launchpad = builder.create()) {

			Bundle start = launchpad.bundle()
				.bundleActivator(Activator.class)
				.start();
		}
	}

When the test is run the output is:

	Hello World
	Goodbye World

## Components

Since the bundle builder can do anything bnd can do, we can also use inner classes for components. These inner
classes must be static and public (this is an OSGi DS requirement). The following is an example of a class
that depends on the `Bar` service. 

    interface Bar {
        void bar();
    }

	@Component
	public static class C {
		@Reference Bar bar;
        @Activate  void activate() { bar.bar(); }
	}

	@Test
	public void component() throws Exception {
		try (Launchpad launchpad = builder
			.bundles("org.apache.felix.log")
			.bundles("org.apache.felix.scr")
			.bundles("org.apache.felix.configadmin")
			.create()) {

			Bundle b = launchpad.component(C.class);
            AtomicBoolean   called = new AtomicBoolean(false);
            launchpad.register(Bar.class, ()-> called.set(true) );
            assertThat(called.get()).isTrue();
		}
	}

Adding a component will return a Bundle. Uninstalling the bundle will remove the component.

## Debugging

Clearly there are lots of things that can go wrong. You can therefore activate the `debug()` on the builder or
Launchpad. This will provide logging to the console.

    @Test
    public void debug() {
		try (Launchpad launchpad = builder
            .debug()
			.create()) {
                            
            }
    }

If you run this the console will show a lot of diagnostics information. 

## Using a bndrun file

One of the great innovations in bndtools is the resolver. So far we've assembled the list of bundles to run ourselves.
However, we can also use a `bndrun` file and `bndrun` files can be resolved.


    @Test
    public void bndrun() {
		try (Launchpad launchpad = builder
            .bndrun("showit.bndrun")
			.create()) {         
            }
    }

If the `bndrun` file contains the following after resolving:

    -runrequires: \
        osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.shell)',\
        osgi.identity;filter:='(osgi.identity=biz.aQute.bnd.runtime.gogo)'


    -runbundles: \
        org.apache.felix.gogo.runtime;version='[1.1.0,1.1.1)',\
        org.apache.felix.gogo.shell;version='[1.1.0,1.1.1)',\
        biz.aQute.bnd.runtime.gogo;version=snapshot,\
        org.apache.felix.log;version='[1.0.1,1.0.2)'
    -runfw: org.apache.felix.framework;version='[5.6.10,5.6.10]'
    -runee: JavaSE-1.8

Then we see the following output:

    Welcome to Apache Felix Gogo
    g!

## Making Bundles

The Launchpad object contains a special bundle builder. It provides the same capabilities that bnd already has when
it creates bundles. You can use all the facilities that you can use in the `bnd.bnd` file. Export-Package, Import-Package,
-includeresource, etc.

The following example shows how to create a bundle with a a special header.

	@Test
	public void bundles() throws Exception {
		try (Launchpad launchpad = builder.create()) {
			Bundle b = launchpad.bundle()
				.header("FooBar", "1")
				.install();
			String string = b.getHeaders()
				.get("FooBar");
			assertThat(string).isEqualTo("1");
		}
	}

### Inheriting from the project, workspace, or a bnd file

When you create a bundle from the Launchpad class then by default you inherit nothing from the enironment. However,
it is possible to set the parent of the builder. The `parent(String)` or `parent(File)` method can set multiple
parents. 

    parent ::= FILE* ( WORKSPACE | PROJECT )?
        

* `WORKSPACE` – Inherit from the workspace, excludes inheriting from the project and must be specified last in the set of parents.
* `PROJECT` – Inherit from the workspace, excludes inheriting from the project and must be specified last in the set of parents.
* `FILE`    – A file path with forward slashes on all platforms. This must point to a bnd/properties file.  

The order of the parents is important. Earlier bnd files override the same named value in later bnd files.

Example:

    Bundle b = lp.bundle().parent('foo.bnd').parent(WORKSPACE).start();


## Hiding a Service

In some scenarios you'd like to hide a service so you can override it with a mocked version. Hiding in OSGi
can be achieved with the [Servic Hooks] services. The easiest way is to hide a service via the Launchpad Builder.

    	LaunchpadBuilder	builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework").hide(SomeService.class);

If you now create a framework, all services _not_ registered via Launchpad will be invisible to all bundles. That is,
only services registered through Launchpad can be seen by the other bundles in the OSGi framework.

	@Test
	public void testHidingViaBuilder() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
				.create()) {

			boolean isHidden = fw.getServices(String.class)
					.isEmpty();			
			assertThat(isHidden).isTrue();

			fw.framework.getBundleContext()
					.registerService(String.class, "fw", null);

			isHidden = fw.getServices(String.class)
					.isEmpty();
			assertThat(isHidden).isTrue();

			ServiceRegistration<String> visibleToAllViaTestbundle = fw.register(String.class, "Hello");

			assertThat(fw.getServices(String.class)).containsOnly("Hello");
			visibleToAllViaTestbundle.unregister();

			isHidden = fw.getServices(String.class)
					.isEmpty();
			assertThat(isHidden).isTrue();
		}
	}

Although hiding via the Launchpad Builder is the easiest way to hide services, it has the disadvantage that all
tests hide the same service(s). It is also possible to handle the service hiding in a more controlled way
by hiding via the Launchpad object.  Using this function does require a bit of orchestration. Once you hide 
a service it becomes invisible to bundles that look for that service later. However, bundles that already 
obtained this service will not lose sight of it. It is therefore necessary to hide a service before the 
corresponding framework is started. Since the default is automatic start, the automatic start must be 
disabled with the `nostart()` method on the builder.

After the framework is then created. the service is hidden and then the framework is started. 

	@Test
	public void testHiding() throws Exception {
		try (Launchpad fw = builder.runfw("org.apache.felix.framework")
				.nostart()
				.create()) {

			@SuppressWarnings("resource")
			Closeable hide = fw.hide(String.class);
			fw.start();

			boolean isHidden = fw.getServices(String.class)
					.isEmpty();
			assertThat(isHidden).isTrue();

			fw.framework.getBundleContext()
					.registerService(String.class, "fw", null);

			isHidden = fw.getServices(String.class)
					.isEmpty();
			assertThat(isHidden).isTrue();

			ServiceRegistration<String> visibleToAllViaTestbundle = fw.register(String.class, "Hello");

			assertThat(fw.getServices(String.class)).containsOnly("Hello");
			visibleToAllViaTestbundle.unregister();

			isHidden = fw.getServices(String.class)
					.isEmpty();
			assertThat(isHidden).isTrue();

			hide.close();
			assertThat(fw.getServices(String.class)).containsOnly("fw");
		}
	}

As you can see from the test code, the `hide` method in this case returns a `Closeable`. This object can be used to
remove the hiding of the given service.

### Visibility

To diagnose any issues it is important to realize that there are some special rules around which bundle does what. Launchpad
has the Bundle Context of the OSGi Framework as well as a special test bundle. The test bundle is empty but it used to have
a Bundle Context for the test code that runs outside the Framework. When you hide a service it will register
Service Hooks that only let services from the test bundle pass through, all other services of the given type are removed
from visibility.

    Closeable Launchpad.hide(SomeService.class);

The service will remain hidden until you close the `Closeable`.



[Service Hooks]: https://osgi.org/specification/osgi.core/7.0.0/framework.servicehooks.html
