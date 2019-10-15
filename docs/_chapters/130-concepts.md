---
layout: default
title: Concepts
---

One of the more surprising things we've learned about modularity in the last decade is how much of a fractal pattern it actually is. All the way from CPU machine instructions all the way up to a large distributed system you can see the pattern of encapsulation to keep as much details as possible confined to the internals of the module. Strangely enough, as an industry we seem to have a hard time learning the lessons from the lower layers where we over time learned the rules to the layers above.

Java was developed the early 90's as a state of the art language based on Object Oriented concepts. It did apply the rules of modularity on the method and class level and to pioneered with providing a higher level module with their concept of _packages_. However, our software industry did their utmost to ignore this concept, helped by the fact that it did have some real shortcomings.

The key shortcoming was that it was hard to keep classes private since they required public visibility when they crossed the package boundary. This collided with the granularity of the package, in general you needed to break an application in many different packages to stay sane as a developer; with the consequence that a lot of private code was forced to become public.

Java did provide a higher level module: the JAR. However, this was a concept outside the language, it was only visible in runtime to be used by the class loaders. Interestingly, it was then used in runtime security but it was kept outside the language that still desperately tried to front a single name space. Problems showed up as class cast exceptions, package private access violations in the same package, and getting the wrong class. It is amazing how brittle the class path model of Java really is.

In search for a model that would allow the management of hundreds of thousands of devices running Java the OSGi decided to leverage the JAR but turn it in to a proper module by adding encapsulation. Since in modularity a module tends to export/import entities that have a lower granularity (classes export/import methods, packages import/export classes), the OSGi solution was to export/import packages.

Package imports/export was a much maligned choice because the maligners did not understand that some of the lessons learned from object oriented programming, where the class is the module, also applied to the other kinds of modules. The greatest problem with Object Oriented was that direct object references created highly coupled systems that were very brittle. Direct references aggregate many constraints that are only important for a specific implementation. This problem was not fully solved until we got Java _interfaces_. Java interfaces allow a user of that interface and an implementer of that interface to only couple on the relevant details necessary for the collaboration; any implementation details on either side of the interface are not constraining the other side in any way. 

What the maligners do not see is that we face the exact same problem when we turn JAR into modules. The maven model only supports direct references between modules and they aggregate implementation constraints in the same way as object references did. The solution the OSGi came up with is to treat a package as a _specification_, just like a Java interface specifies the behavior of an implementation class. 

In this way a bundle (the OSGi JAR module) could provide an implementation for a (specification) package and other bundles could consume this implementation. Again, this is similar to the Java interface for classes. 

In the Java world we learned rather slowly that interfaces are great but that you have this pesky problem of how to get an instance for that interface if you should not be coupled to a specific implementation? Java created a a number of quite horrendous _factory_ models for almost each subsystem, a factory model that too often bled into the actual collaborative API. For example, the `javax.persistence` package does not only contain the classes to work with JPA, it also contains a surprisingly large API to manage the life cycle of the provider. Factories also heavily abused class loaders for a purpose they were clearly not designed for. In Java 6 Sun tried to stop the hemorrhaging with the Service loader but forgot that class loaders are not designed for this purpose and baked them right into the API. 

Spring brought dependency injection to the masses that allowed the consumers and providers to stay away from each other but it then introduced a massive coupling in the XML that contained too many details about the provider implementations since it specified the provider class names. 

The solution OSGi came up with in 1998 was to use a _service broker_. A broker makes sure that consumers and providers were bound together once they are available. This is the only true modular model that allows all implementation details to remain private inside the module. 

<embed class="illustration" src="{{ '/img/chapters/concepts-service.svg' | prepend:site.baseurl }}" />

A the consequence of this model is that services, an implementation object implementing a Java interface from a specification package, cannot be assumed to be always there. Where Spring calculates the dependencies of its beans to initialize a system in the right order, a broker negotiates when a service is available since it does know any of its implementation details. One of those implementation details is when that service will be available. The broker is therefore dynamic, it can react to changing circumstances, another even more both maligned and misunderstood OSGi feature.

The hardest part of OSGi is to look at it with a clean slate. The core model is surprisingly simple, elegant, and extremely powerful. The hard part is that it tends to conflict (as it should!) with all the bad habits in existing code bases that grew up thinking class loaders were simple extension mechanisms.

## Bundles

So to summarize, a bundle is a JAR that imports and exports a set of packages. These imports and exports are bound to other bundles when they are resolved, allowing multiple versions of the same package. Once a bundle is started, it can  then communicate with the external world at its own discretion; it can also get and register services to collaborate with other bundles. The following picture depicts the model of an OSGi application:

<embed class="illustration" src="{{ '/img/chapters/concepts-bundles.svg' | prepend:site.baseurl }}" />

The symbols used are defined in the OSGi specifications. The rectangle with rounded corners is a bundle, the triangle is a service (it always points in the dependency direction, i.e. the Reporter bundle depends on the sensor service to be there. Input and output is depicted with the corresponding flow chart symbol.

In a perfect OSGi world, packages are just a minor detail. Until then, we also need some way to show package imports and package exports. In the OSGi specification, we use an open rectangle for imported packages and and black rectangle for exported packages. Private packages are depicted with a grey rectangle, see:

<embed class="illustration" src="{{ '/img/chapters/concepts-packages.svg' | prepend:site.baseurl }}" />

## Components

There is often a confusion of terminology, like, is a bundle a component? We are guilty of not always using the terminology consistent. However, in the past few years it has become very clear that the OSGi declarative services provides a programming model that should have been incorporated in the OSGi framework from the beginning. DS allows you to make any object _active_ with a simple annotation. This object can automatically be registered as a service. If there are dependencies on other services then you can easily specify those dependencies with an annotation on a set method.

In DS, the implementation class is called the _component_ and in this document we follow this lead. Therefore, in general a bundle consists of a number of components. 

# The Workspace

The bndlib _workspace_ is an encapsulation of a set of cohesive _projects_, where a project exports zero or more bundles via _repositories_. A repository provides access to set of bundles exported by some means, likely from other workspaces. A repository can be on the local file system or a remote system like Maven central. 

Projects can depend on other projects in the workspace or import bundles from the repositories. This is depicted in the following figure.

<embed class="illustration" src="{{ '/img/chapters/concepts-workspace.svg' | prepend:site.baseurl }}" />

A workspace is a single directory, just like a git workspace it encompasses all its sub directories. Though the name of the workspace directory is free to choose, it is highly recommended to use a naming strategy. In practice you will create many different workspaces and having a naming strategy will significantly simplify the handling of these workspaces.

On the same root level in the workspace as the `cnf` directory, bndlib expects the projects. Yes, projects *must* reside in the root level. The reason is again, simplicity. We will later discuss how the bundle's symbolic name is derived from the project's directory name. Since projects can depend on each other, bndlib maintains a workspace repository of projects that it derives from the top level directories. Some people desperately want to use hierarchies of projects (often because that is how they used to work before). However, even people patching bndlib to make it hierarchical admit that the simple linear model is actually working quite well. The reason it works so well is that a workspace is not supposed to hold every single bundle that your organization produces. It is intended to be a cohesive set of between 10-20 up to a couple of hundred projects.
