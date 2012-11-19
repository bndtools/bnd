---
title: Bndtools Tutorial
description: Introduction to component development with Bndtools.
author: Neil Bartlett
---

Introduction
============

In this tutorial we will build a sample application composed of two components and an API. The following diagram shows the bundle architecture (simplified):

![]($images$bundles.png)

In the tutorial we create the top three bundles (rectangles):

 *	The API bundle exports a service interface, `Greeting`.
 *	The Provider bundle imports the interface and publishes an instance of the service.
 *	The Command bundle imports the interface and binds to the service instance, and also publishes a `Command` service that is used by the Felix Shell bundle.
 
Installing Bndtools
===================

Please refer to the [Installation Instructions](installation.html).

Create an API Project
=====================

First we need to create a Bndtools OSGi Project. This is just a standard Eclipse Java Project, with an additional builder for constructing OSGi bundles.

1. From the File menu, select **New -> Bndtools OSGi Project**.

	![]($images$01.png)

1. On the next page, enter `org.example.api` as the name of the project. Select at least J2SE-1.5 for the JRE execution environment.

	![]($images$02.png)

1. Next you are offered a choice of project templates to start off your project. Select **Empty Project** and click **Finish**. The new project will be created.

	![]($images$03.png)

1. If this is the first time you have used Bndtools in this workspace, you will now see the "Welcome" dialog. Click **Next** followed by **Finish** to allow Bndtools to setup a configuration project and import a basic repository. A repository is a place where bundles that you use in your projects are stored. A remote "BndTools hub" repository is created by default that contains some often used bundles.

	![]($images$04.png)

	![]($images$05.png)



*Important Points:*

* Bndtools projects are based on standard Eclipse Java (JDT) projects.
* Bndtools uses a `cnf` project containing workspace-wide configuration that is normally shared between developers. It may also contain a repository of bundles.
* A file named `bnd.bnd` is created at the top of each Bndtools project, which controls the settings for the project. The same settings are used by bnd when it is invoked from an offline ANT build.

Write and Export the API
-------------------------

OSGi offers strong decoupling of producers and consumers of functionality. This is done by encouraging an API-based (or in Java terms, interface-based) programming model, where producers of functionality implement APIs and the consumers of functionality bind only to APIs, not any particular implementation. For our example we will use a fairly trivial API.

In the `src` directory of the new project, create a package named `org.example.api`. In the new package create a Java interface named `Greeting`, as follows:

~~~~~~ {.Java}
package org.example.api;

public interface Greeting {
    String sayHello(String name);
}
~~~~~~


Define the Bundle
-----------------

The project we have created defines a single bundle with a Bundle Symbolic Name (BSN) of `org.example.api` (i.e., the same as the project name). As soon as we created the project, a bundle file named `org.example.api.jar` was created in the `generated` directory, and it will be rebuilt every time we change the bundle definition or its source code.

However, the bundle is currently empty, because we have not defined any Java packages to include in the bundle. This is an important difference of Bndtools with respect to other tools: bundles are always empty until we explicitly add some content. You can verify this by double-clicking the bundle file and viewing its contents: it will have only `META-INF/MANIFEST.MF` and `OSGI-OPT/bnd.bnd` entries.

We want to add the package `org.example.api` to the exported packages of the bundle. So open the `bnd.bnd` file at the top of the project and select the **Contents** tab. Now the package can be added in one of two ways:

*	Click the "+" icon in the header of the **Export Packages** section, then select `org.example.api` from the dialog and click **OK**... *or*
*	Drag-and-drop the package `org.example.api` from Eclipse's Package Explorer view into the **Export Packages** list.

(TIP: Advanced users may prefer to enter `Export-Package: org.example.api` manually in the **Source** tab).

As soon as this is done, a popup dialog appears titled "Missing Package Info". This dialog is related to package versioning: it is asking us to declare the version of this exported package. Click **OK**.

![]($images$06.png)

The **Contents** tab should now appear as in the following screenshot:

![]($images$07.png)

Save the file, and the bundle will be rebuilt to include the selected export. We can confirm by opening the **Imports/Exports** view and selecting the bundle file in the **Package Explorer**. Note the package has been assigned version 1.0.0:

![]($images$08.png)

*Important Points:*

*	The project configuration and the bundle contents are defined by `bnd.bnd`.
*	The identity of a bundle -- its "Bundle Symbolic Name" or BSN -- is controlled by the project name. In this case, the bundle's BSN is equal to the project name.
*	Bundles are always empty until we explicitly add contents to them. Adding a package to the **Export Packages** panel included that package in the bundle, and also declared it as an export in the `META-INF/MANIFEST.MF`.
*	*Normally* bundles contain more than just a single interface. This example is intentionally simplistic.


Create an Implementation Project
================================

We will now create another project that defines two bundles: a provider and a client of the `Greeting` API.


Create the Project
------------------

Create another Bndtools project, named `org.example.impls`. At the **Project Templates** step, select **Component Development (Declarative Services)** and click **Finish**.

![]($images$09.png)

Add the API as a Build Dependency
---------------------------------

We need to add the API project as a build-time dependency of this new project.

The `bnd.bnd` file of the newly created project will have opened automatically. Click the **Build** tab and add `org.example.api` in either of the following ways:

 *	Click the "+" icon in the toolbar of the **Build Path** panel. Double-click `org.example.api` under "Workspace" in the resulting dialog; it will move over to the right-hand side. Click **Finish**
 
	![]($images$10.png)

 *	**OR** drag-and-drop `org.example.api` from the **Repositories** view into the **Build Path** panel.

In either case, the `org.example.api` bundle will appear in the **Build Path** panel with the version annotation "latest":

![]($images$11.png)

*Important Points:*

 *	Build-time dependencies of the project can be added in the **Build Path** panel of the `bnd.bnd` editor.
 *	Adding dependencies in this way (i.e. rather than via Eclipse's existing "Add to Build Path" menu) ensures that exactly the same dependencies are used when building offline with ANT.

Write an Implementation
-----------------------

We will write a class that implements the `Greeting` interface. When the project was created from the template, Java source for a class named `org.example.ExampleComponent` was generated. Open this source file now and make it implement `Greeting`:

~~~~~~ {.Java}
@Component
public class ExampleComponent implements Greeting {

	public String sayHello(String name) {
		return "Hello " + name;
	}

}
~~~~~~

Note the use of the `@Component` annotation. This enables our bundle to use OSGi Declarative Services to declare the API implementation class. This means that instances of the class will be automatically created and registered with the OSGi service registry. However the annotation is build-time only, and does not pollute our class with runtime dependencies -- in other words, this is a "Plain Old Java Object" or POJO.

Test the Implementation
-----------------------

We should write a test case to ensure the implementation class works as expected. In the `test` folder, a test case class already exists named `org.example.ExampleComponentTest`. Write a test method as follows:

~~~~~~ {.Java}
public class ExampleComponentTest extends TestCase {

	public void testSaysHello() throws Exception {
		String result = new ExampleComponent().sayHello("Bob");
		assertEquals("Hello Bob", result);
	}
}
~~~~~~

Now right-click on the file and select **Run As > JUnit Test**.

![]($images$12.png)

Verify that the **JUnit** view shows a green bar. If not, go back and fix the code!

Note that, since this is a unit test rather than an integration test, we did not need to run an OSGi Framework; the standard JUnit launcher is used. Again, this is possible because the component under test is a POJO.

Build the Implementation Bundle
-------------------------------

As in the previous project, a bundle is automatically built based on the content of `bnd.bnd`. In the current project however, we want to build *two* separate bundles. To achieve this we need to enable a feature called "sub-bundles".

Right-click on the project `org.example.impls` and select **New > Bundle Descriptor**. In the resulting dialog, type the name `provider` and click **Finish**.

A popup dialog will ask whether to enable sub-bundles. Click **OK**.

![]($images$13.png)

Some settings will be moved from `bnd.bnd` into the new `provider.bnd` file. You should now find a bundle in `generated` named `org.example.impls.provider.jar` which contains the `org.example` package and a Declarative Services component declaration in `OSGI-INF/org.example.ExampleComponent.xml`.

*Important Points:*

 *	Bndtools project can output either a single bundle or multiple bundles.
 *	In the case of single-bundle projects, the contents of that bundle are defined in `bnd.bnd`.
 *	In the case of multi-bundle projects, the contents of each bundle is defined in a separate `.bnd` file. The `bnd.bnd` file is still used to define project-wide settings, such as build dependencies.

Run an OSGi Framework
=====================

We'd now like to run OSGi. To achieve this we need to create a "Run Descriptor" that defines the collection of bundles to run, along with some other run-time settings.

Right-click on the project `org.example.impls` and select **New > Run Descriptor**. In the resulting dialog, enter `run` as the file name and click **Next**. The next page of the dialog asks us to select a template; choose **Apache Felix 4 with Gogo Shell** and click **Finish**.

![]($images$14.png)

In the editor for the new `run.bndrun` file, click on **Run OSGi** near the top-right corner. Shortly, the Felix Shell prompt "`g! `" will appear in the **Console** view. Type the `lb` command to view the list of bundles:

  g! lb
  START LEVEL 1
     ID|State      |Level|Name
      0|Active     |    0|System Bundle (4.0.3)
      1|Active     |    1|Apache Felix Gogo Runtime (0.10.0)
      2|Active     |    1|Apache Felix Gogo Shell (0.10.0)
      3|Active     |    1|Apache Felix Gogo Command (0.12.0)
  g!

Next we want to include the `org.example.impls.provider` and `osgi.cmpn` bundles. This can be done as follows:

 *	Click the "+" icon in the toolbar of the **Run Requirements** panel. In the dialog, double-click `org.example.impls.provider` and `osgi.cmpn` under "Workspace" and click **Finish**.

Also add the osgi.cmpn bundle. The **Run Requirements** panel should now look like this:

![]($images$15.png)

Check **Auto-resolve on save** and then save the file. Returning to the **Console** view, type `lb` again:

  g! lb
  START LEVEL 1
     ID|State      |Level|Name
      0|Active     |    0|System Bundle (4.0.3)
      1|Active     |    1|Apache Felix Gogo Runtime (0.10.0)
      2|Active     |    1|Apache Felix Declarative Services (1.6.0)
      3|Active     |    1|org.example.impls.provider (0.0.0)
      4|Active     |    1|osgi.cmpn (4.2.0.200908310645)
      5|Active     |    1|Apache Felix Gogo Shell (0.10.0)
      6|Active     |    1|Apache Felix Gogo Command (0.12.0)
      7|Active     |    1|org.example.api (0.0.0)
  g!

The provider bundle has been added to the runtime dynamically. Note that the API bundle and Apache Felix Declarative Services are also added because they resolved as dependencies of the provider.

We can now look at the services published by our provider bundle using the command `inspect service capability 3`... (or the short form `inspect s c 3`):

	g! inspect c service 2
  org.example.impls.provider [2] provides:
  service; org.example.api.Greeting with properties:
     component.id = 0
     component.name = org.example.ExampleComponent
     service.id = 6
  g!

Our bundle now publishes a service under the `Greeting` interface.


*Important Points:*

 *	Run-time configurations can be defined in a `.bndrun` file. Multiple different run configurations can be used, resulting in different sets of bundles, different OSGi Framework implementations etc.
 *	The set of bundles to include is derived from the **Run Requirements** list. Bndtools uses OBR resolution to resolve a list of bundles including their static dependencies.
 *	If the OSGi Framework is still running, then saving the `bndrun` file will cause the list of bundles to be dynamically updated. So we can add and remove bundles without restarting.
 *	Editing an existing bundle -- including editing the Java code that comprises it -- will also result in the bundle being dynamically updated in the runtime.


Write a Command Component
=========================

Finally we will write a component that consumes the Greeting service and publishes a shell command that can be invoked from the Felix shell.

First we need to make the Felix shell API available to compile against. Open `bnd.bnd` and change to the **Build** tab. Add `org.apache.felix.gogo.runtime` to the list of build dependencies, and save the file:

![]($images$17.png)

Now create a new Java package under the `src` folder named `org.example.command`. In this package create a class `GreetingCommand` as follows:

~~~~~~~~~~ {.Java}
import org.example.api.Greeting;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component(properties={
		"osgi.command.scope=example", 
		"osgi.command.function=greet"}, 
		provide=Object.class)
public class GreetingCommand {

	private Greeting greetingSvc;

	@Reference
	public void setGreeting(Greeting greetingSvc) {
		this.greetingSvc = greetingSvc;
	}

	public void greet(String name) {
		System.out.println(greetingSvc.sayHello(name));
	}
}
~~~~~~~~~~

Create a Bundle for the Command Component
-----------------------------------------

The command component is not part of the provider bundle, because it lives in a package that was not included. We could add it to the provider bundle, but it would make more sense to create a separate bundle for it.

Right-click again on the `org.example.impls` project and select **New > Bundle Descriptor** again. Enter the name as `command` and click **Finish**.

Add the package `org.example.command` to the **Private Packages** panel of the newly created file. As before, this can be done using the "+" button in the toolbar or by drag-and-drop.

We also need to declare that the bundle contains Declarative Services components. Change to the **Components** tab of the editor and click the **Add** button. In the name field, enter "*" (i.e. asterisk). Now save the file.

Add the Command Bundle to the Runtime
-------------------------------------

Switch back to the editor for `run.bndrun`. In the **Run Requirements** tab, add the `org.example.impls.command` bundle, and save the file.

The command bundle will now appear in the list of bundles when typing `lb`:

~~~~~~~~~~
	g! lb
  START LEVEL 1
     ID|State      |Level|Name
      0|Active     |    0|System Bundle (4.0.3)
      1|Active     |    1|Apache Felix Declarative Services (1.6.0)
      2|Active     |    1|org.example.impls.provider (0.0.0)
      3|Active     |    1|Apache Felix Gogo Runtime (0.10.0)
      4|Active     |    1|osgi.cmpn (4.2.0.200908310645)
      5|Active     |    1|Apache Felix Gogo Shell (0.10.0)
      6|Active     |    1|Apache Felix Gogo Command (0.12.0)
      7|Active     |    1|org.example.impls.command (0.0.0)
      8|Active     |    1|org.example.api (0.0.0)
  g!
~~~~~~~~~~
Finally, the `greet` command will now be available from the Gogo shell:

	g! greet BndTools
  Hello BndTools
  

