---
title: Versioning
layout: default
requires: 1.15
---


Versioning is probably the most painful part of developing real software. Where toys and prototypes can be developed ignoring evolution, real software requires a migration path to an unknown future.

The OSGi has defined a versioning policy that is described in the [Semantic Versioning whitepaper](https://docs.osgi.org/whitepaper/semantic-versioning/). bnd fully supports this model and provides many shortcuts. The goal of bnd is remove any manual work from versioning bundles as well as packages.

The key concept to version in OSGi is the ''package''. Bundles are an ''aggregate'' and therefore must move as fast as the fastest moving exported packages they contain. For example, if a bundle contains two exported packages `foo` and `bar` and `foo` is not changed but `bar` has a major change, then the bundle version needs to also have a major change. This requires an unnecessary update for a bundle that only depended on `foo`. Aggregating dependencies increases the fan out of the transitive dependencies. The result is that systems can only evolve when everything is updated simultaneously. The result is that the system as a whole becomes brittle.

In contrast, versioning the packages and using Import-Package, bundles can be refactored and versioned independently. 

## Best Practices

* Version on packages, not on bundles
* Create a packageinfo file with "version 1.0" in each package directory and maintain it meticuously for each change to the package. Any change that breaks consumers, increment major. Changes that break providers (if any), increment minor. Other changes micro. bnd will then properly version this package when exported
* If you provide an API (as defined in te Semantic Versioning whitepaper) export the API package even if it is not in your project and put the `provide:=true` directive on the package export, e.g. `Export-Package: org.osgi.service.event; provide:=true`.
* Every time when you release a bundle to the external world, bump the minor part of the bundle  version. For internal development releases, bump the micro part of the bundle version.

## Versions in OSGi
A version in OSGi has 4 parts:

  major        1
  minor        1.1
  micro        1.1.1
  qualifier    1.1.1.qualifier

To survive versioning, one must have a ''version policy''. A version policy puts semantics on the version numbers. The ''recommended'' policy in OSGi is changing the part when there is:

  major        a breaking change
  minor        a backward compatible changes
  micro        a bug fix (no API change)
  qualifier    a new build

In OSGi, the decision was taken to have a single export version. The import statement allows a version range to be set. For example:

    Export-Package: com.acme.foo; version=1.0.2
    Import-Package: com.acme.bar; version="[1,2)"

The semantic versioning white paper introduces two terms that are orthogonal to the imports and exports as well as implementing or delegating:

* Provide an API - Provide the functionality defined in an API
* Consume an API - Consume the functionality defined in an API

Provide and consume is orthogonal to implementing an interface and delegating. For example, the Configuration Admin service has the `ConfigurationAdmin` interface that is implemented by the Provider of an API but the `ConfigurationListener` interface is implemented by the Consumer of the API.

The reason for the providers and consumer terms is that version policies are different. A change in an API almost always affects the provider but with careful API design it is often possible to make a change backward compatible for consumers.

In bnd, whenever you have to provide an import range, you can use modifiers to create a range out of a single version:

* `@1.2.3` – Creates an import up to the next major version: `[1.2.3,2.0.0)`.
* `1.2.3@` – Creates an import up to the next minor version: `[1.2.3,1.3.0)`.
* `=1.2.3` – Creates a range that only accepts that version: `[1.2.3,1.2.3]`.

## Versioning Packages
If you have a package that is containing implementation code that is supposed to be directly used by the consumers then this is a ''library''.
A library package is not an API that can be implemented by other bundles, it is the implementation. Then the versioning of library packages is relatively straightforward: Any change that breaks a consumer of that package must increment the major version number. For example, if the popular ASM library would add a method to the `MethodVisitor` class then it must increment the major version number of the `org.objectweb.asm` package because all existing consumers of this library would then be broken.

If however a package contains an API that is provided and consumed by others the situation is more complex. In such a case, the provider should export the API package and the consumers should import it.

bnd explicitly allows the inclusion of packages that come from other projects. It is just good practice to include an API package in your bundle if you are the provider of that API. However, this means that maintaining the version of the package in the manifest is ''wrong'', it would have to be maintained in several places, which is very error prone.

For this reason, bnd provides a way to store the version of the package together with the package itself. One with annotations and one without when annotations are not possible.

The @Version annotation is placed on the package. Since Java 5 it is possible to create a package-info.java file that can be used to annotate packages:

    package-info.java:
        @org.osgi.annotation.versioning.Version.Version("1.2.0")
        package com.example;

A non-annotation based alternative is the `packageinfo` file. When bnd scans the Java archives it will look in each package for this packageinfo file. The format of this file is very simple:

    packageinfo:
        version 1.2.0

If you now export the package (from any bundle that has the package on its class path), it will be properly versioned.

    bnd.bnd:
        Export-Package: com.example.*

The resulting manifest will look like:

    Manifest:
        Export-Package: com.example; version=1.2.0

If you export a package from another bundle, bnd will also look in the manifest of that other bundle for a version.

Using packageinfo (or the @Version annotation) is highly recommended.

## When Package Version Differ

The package version for package p can come from the following places (in order of increasing priority):

* `p/packageinfo`
* `p/package-info.java` with an annotation
* Manifest of the source bundle
* Manifest of the bundle under construction, i.e. the bnd.bnd file

The bnd warning means that bnd finds multiple definitions of the version for p and they are not the same. So I assume you set the version in the manifest of the bundle under construction and in one of those other places. The best is to remove the version from your bnd.bnd file. The by far absolute best way is to only set the version of the package in the package directory (either `packageinfo` or `package-info.java` with an annotation).

## Import Version Policy
If you import a package bnd will look at the exported version of that package. This version is not directly suitable for the import because it is usually too specific, it needs a policy to convert this export version to an import version.

An importer that provides the functionality of an API package is much closer tied to that package than a client. The whitepaper recommends binding to the major.minor part of the version for a provider. That is, any change in the minor part of the version breaks the compatibility. This makes sense, the provider of an API must implement the contract and is therefore not backward compatible for any change in the API. A consumer of the API only has to be bound to the major part because it is much more relaxed for the backward compatibility.

For example, a new method is added to an interface that is implemented by the provider of the API. Old clients have no visibility of this method because when they compiled it did not exist. However, the provider of the API must be modified to implement this method otherwise more modern clients would break.

This asymmetry creates the need for two version policies:

    -provider-policy :    ${range;[==,=+)}
    -consumer-policy :    ${range;[==,+)}

The given values are the defaults. The value of the version policy will be used calculate the import based on the exported package. The [`${range}`](../macros/range.html) macro provides a convenient shortcut to do this using a version mask.

For example, a bundle that implements the OSGi Event Admin service can use the following bnd file:

    bnd.bnd:
        Private-Package: com.example.impl.event

The resulting manifest would look like:

    Manifest:
        Import-Package:  org.osgi.service.event; version="[1.1,2)", ...
        ...

How does bnd know if a bundle is a provider or a consumer of a specific package? 

Well, the default is the consumer policy but this can be overridden with the `provide:=true` directive that works on the `Import-Package` clauses as well as on the `Export-Package` clauses.

The `provide:` directive indicates to bnd that the given package contains API that is provided by this bundle. The (strongly) recommended way is to put the `provide:=true` directive on the `Export-Package` header, even if the package comes from another bundle. This way the bundle contains a copy of the package that is by default imported with the proper provider policy range.

Also see [-consumerpolicy](../instructions/consumer_policy.html), [-providerpolicy](../instructions/provider_policy.html) and [range](../macros/range.html).

For example, an implementation of the OSGi Event Admin specification could use the following bnd file:

    bnd.bnd:
        Export-Package:  org.osgi.service.event; provide:=true
        Private-Package: com.example.impl.event

The resulting manifest would look like:

    Manifest:
        Export-Package:  org.osgi.service.event; version=1.1
        Import-Package:  org.osgi.service.event; version="[1.1,1.2)", ...
        ...

If for some reason it is not desirable to export the API package in the implementation bundle, then the `provide:` directive can also be applied on the `Import-Package` header:

    bnd.bnd
        Import-Package: org.osgi.service.event; provide:=true, *
        Private-Package: com.example.impl.event

The resulting manifest would look like:

    Manifest:
        Import-Package:  org.osgi.service.event; version="[1.1,1.2)", ...
        ...

## Substitution
A key aspect of OSGi is that a package can be both imported and exported. The reason is that this feature allows a framework more leeway during resolving without creating multiple unconnected class spaces.

After the bundle has been created and analyzed bnd will see if an exported package is eligible for import. An export is automatically imported when the following are true:

* There exists at least one reference to the exported package from a private package
* The exported package has no references to any private packages
* The exported package does not have a `-noimport:` directive.

If a package is imported it will use the version as defined by the version policy.

## Versioning Bundles
Versioning bundles usually requires bumping the version every time it is placed in a repository. When package versioning is used, the bundle version is only important for tracking an artifact.


## Why does bnd use only major and minor version component in import-package headers?

The micro is left out because it generates a lot of unnecessary releases, this is similar to the maven release process. If you connect everything 100%, you cannot move anything unless all its dependencies are moved at the same time. We actually tried in the OSGi build to use micro version changes for default methods in Java 8 but found that it just creates an enormous ripple through effect in the build. Not depending on the micro version is a lubricant that does not kill any bundle out there that depends on you.

This should not be a problem because a micro version is a deployment issue since the semantic versioning should be used for APIs and a micro change is not visible in the API. 

That said, this is bnd so obviously you can override it. You can override the default version policy is:

	-provider-policy = ${range;[==,=+)}
	-consumer-policy = ${range;[==,+)}

Just set '===' instead of '==' for the floor version in your pom.xml in the <configuration> section and you should be ok.

	<configuration>
		<_provider-policy>${range;[===,=+)}</_provider-policy>
		<_consumer-policy>${range;[===,+)}</_consumer-policy>
	</configuration>

