---
order: 230
title: Bundle Annotations
layout: default
---

Manifest headers are challenging to keep in sync with the code in the bundle. It often takes several attempts to get all the details correct.

One of the goals of bnd is to eliminate such issues by relying on Java's type system to express the semantics of OSGi metadata.

## Bundle Annotations

To address this bnd developers pioneered _manifest annotations_ which evolved into OSGi's [_bundle annotations_](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle). A **bundle annotation** is used to express metadata that cannot otherwise be derived from code.

A *bundle annotation* is applied to a type or package and when processed by bnd will cause the generation of corresponding manifest headers (and header clauses). Generating manifest headers from type safe structures is far less likely to result in errors, simplifies the developers life and is more conducive to code refactoring which won't result in information loss.

The following example shows the _preferred way_ to handle package versioning by applying the [`@Export`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Export) and [`@Version`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.versioning.Version) _bundle annotations_ to `com/acme/package-info.java`.

```java
@Export
@Version("1.3.4")
package com.acme;
```

which results in the manifest header:

```
Export-Package: com.acme;version="1.3.4"
```

### @Requirement & @Capability Annotations

Though Java class files contain enough information to find code dependencies, there are many dependencies that are indirect. OSGi _extenders_ for instance are often a requirement to make a bundle function correctly but often client bundles have no code dependency on the extender. For example, Declarative Services (DS) went out of its way to allow components to be Plain Old Java Objects (POJO). The result is that resolving a closure of bundles starting from a DS client bundle would not drag in the Service Component Runtime (SCR), resulting in a satisfied but rather idle closure.

The solution was to describe the requirement for the runtime SCR dependency using [Requirements and Capabilities](https://osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.dependencies). But again, writing these complex clauses in the manifest by hand is both error prone and painful.

The [`@Requirement`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Requirement) and [`@Capability`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Capability) annotations were designed to address this issue. These annotations can be used to create [*custom bundle annotations*](#custom-bundle-annotations), described later on. Let's discuss the DS example.

Recent DS specifications require implementations to provide the following capability:

```
Provide-Capability: osgi.extender;
    osgi.extender="osgi.component";
    version:Version="1.4.0";
    uses:="org.osgi.service.component"
```

While this provides a capability that can be required, we need a requirement to be generated from client code that uses DS. Enter recent versions of DS annotations which are meta-annotated with `@RequireServiceComponentRuntime`, a _custom bundle annotation_ which is specified as:

```java
@Requirement(
    namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
	name = ComponentConstants.COMPONENT_CAPABILITY_NAME,
	version = ComponentConstants.COMPONENT_SPECIFICATION_VERSION)
public @interface RequireServiceComponentRuntime { }
```

If you inspect the source code for `@Component` you'll find it is meta-annotated with `@RequireServiceComponentRuntime`. When you write a DS component using `@Component` as follows

```java
@Component
class Foo { ... }
```

and because of the inherent _bundle annotations_ it holds, the following manifest clause is generated

```
Require-Capability: \
	osgi.extender; \
	filter:="(&(osgi.extender=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))"
```

The invisible link created between user code and the indirect requirement is a powerful mechanism that enables automatic validation of a bundle closure.

### Arbitrary Manifest Headers

_Bundle annotations_ aren't just about package versioning or requirements and capabilities. They are about lifting *metadata* out of our code to avoid, among other things, error prone duplication of information. A common example is the bundle activator. Bundle Activators are require to be described in a manifest header. This association is not visible to refactoring tools and as such can easily end up out of sync.

The [`@Header`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Header) annotation exists to address this problem.

```java
package com.acme;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public Activator implements BundleActivator { ... }
```

results in the manifest header:

`Bundle-Activator: com.acme.Activator`


### Macros

You'll note the string `${@class}` used in the above example. String fields in *bundle annotations* are processed through bnd's macro processor. This macro processor provides access to all default and builder macros. *More info on bnd macros can be found in the [macros chapter](/chapters/850-macros.html).*

Bnd also provides access to certain key properties of the current processing state.

- ***@class*** - gives the fully qualified name of the annotated class
- ***@class-short*** - gives the simple name of the annotated class
- ***@package*** - gives the package of the annotated class
- ***@version*** - gives the package version of the annotated class

The `@Header` example above used the macro `${@class}` which lifted the `@class` property holding the class name of the activator into the header to avoid having to duplicate it. This also means that refactoring the activator won't cause the manifest to get out of sync.

### Custom Bundle Annotations

Certain *bundle annotations* have a second important use. We know that if applied to a type or package *bundle annotations* result in a clause in the manifest. However, many can be used as meta-annotations to a second annotation. The second annotation is considered a ***custom bundle annotation***. The *custom bundle annotation* results in a manifest clause only when applied to a type or package.

This makes it possible to create an annotation for a subsystem. For example, an annotation `@ASL_2_0` that sets the `Bundle-License` header to the Apache Software License version 2.0.

```java
@BundleLicense(
    name = "http://www.opensource.org/licenses/apache2.0.php",
    link = "http://www.apache.org/licenses/LICENSE-2.0.html",
    description = "Apache Software License 2.0")
@interface ASL_2_0 {}

// takes effect when applied to a type

@ASL_2_0
class Foo { ... }
```

#### Adding Attributes and Directives

When creating *custom bundle annotations* a common requirement is to make them parameterizable such that the values of the *custom bundle annotation* feed into the header clauses resulting from the *bundle annotation* applied to it (*remember; a **custom bundle annotation** is meta-annotated with a **bundle annotation***.)

[OSGi](https://osgi.org/specification/osgi.core/7.0.0/) specifies two annotations, [`@Attribute`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Attribute) and [`@Directive`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Directive), for this purpose. Any methods of the **custom bundle annotation** annotated with `@Attribute` or  `@Directive` will result in those becoming additional attributes or directives respectively of the resulting header clause _when a value is supplied_.

##### `@Attribute`

`@Attribute` allows you to add new or update existing attributes from the _bundle annotation_.

```java
@Capability(namespace = "foo.namespace")
@interface Extended {
	@Attribute("foo.attribute") // this attribute enhances the @Capability
	String value();
}

// usage

@Extended("bar")
class Foo {}
```

which results in the manifest header:

`Provide-Capability: foo.namespace;foo.attribute=bar`

##### `@Directive`

`@Directive` behaves similarly; with some caveats. You can add new or update existing directives for namespaces _not_ defined by OSGi specifications.

```java
@Capability(namespace = "foo.namespace")
@interface Extended {
	@Directive("foo.directive")
	String value();
}

// usage

@Extended("bar")
class Foo {}
```

results in the manifest header:

`Provide-Capability: foo.namespace;foo.directive:=bar`

**However**, namespaces defined by OSGi specifications will be validated and will not accept directives which are not part of the spec _unless_ they are prefixed with `x-`.

```java
@Capability(namespace = "osgi.extender", name = "bar", version = "1.0.0")
@interface Extended {
	@Directive("foo")
	String value();
}

// usage

@Extended("bar")
public class Foo {}
```

will result in an error:

`Unknown directive 'foo:' for namespace 'osgi.extender' in 'Provide-Capability'. Allowed directives are [effective:,uses:], and 'x-*'.`

**It should be noted** that it's possible to elide such errors using bnd's [`-fixupmessages`](/instructions/fixupmessages.html) instruction.

This next example however:

```java
@Capability(namespace = "osgi.extender", name = "bar", version = "1.0.0")
@interface Extended {
	@Directive("x-foo")
	String value();
}

// usage

@Extended("bar")
public class Foo {}
```

results in the manifest header:

`Provide-Capability: osgi.extender;osgi.extender=bar;version:Version="1.0.0";x-foo:=bar`

**It should be noted** that default values for methods annotated with `@Attribute` and `@Directive` are deemed to be for documentation purposes only and will not be emitted into resulting headers.

#### Accessor Properties

For more customisation options see chapter on [Accessor Properties](/chapters/235-accessor-properties.html).

### Where to find Bundle Annotations

**OSGi** *bundle annotations* can be found in the `osgi.annotation` bundle.

```xml
<dependency>
  <groupId>org.osgi</groupId>
  <artifactId>osgi.annotation</artifactId>
  <version>7.0.0</version>
</dependency>
```

**Bnd** *bundle annotations* can be found in the `biz.aQute.bnd.annotations` bundle.

```xml
<dependency>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>biz.aQute.bnd.annotation</artifactId>
  <version>${bnd.version}</version>
</dependency>
```

### List of Bundle Annotations

OSGi Bundle Annotations:

- [`@Attribute`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Attribute)
- [`@Capability`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Capability)
- [`@Directive`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Directive)
- [`@Export`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Export)
- [`@Header`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Header)
- [`@Requirement`](https://osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle.Requirement)
- Many OSGi Specifications also define their own _custom bundle annotations_
  - [`@RequireConfigurationAdmin`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.cm.html#org.osgi.service.cm.annotations.RequireConfigurationAdmin)
  - [`@RequireMetaTypeExtender`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html#org.osgi.service.metatype.annotations.RequireMetaTypeExtender)
  - [`@RequireMetaTypeImplementation`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html#org.osgi.service.metatype.annotations.RequireMetaTypeImplementation)
  - [`@RequireServiceComponentRuntime`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#org.osgi.service.component.annotations.RequireServiceComponentRuntime)
  - [`@RequireEventAdmin`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.event.html#org.osgi.service.event.annotations.RequireEventAdmin)
  - [`@RequireJPAExtender`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jpa.html#org.osgi.service.jpa.annotations.RequireJPAExtender)
  - [`@RequireHttpWhiteboard`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.http.whiteboard.html#org.osgi.service.http.whiteboard.annotations.RequireHttpWhiteboard)
  - [`@RequireConfigurator`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html#org.osgi.service.configurator.annotations.RequireConfigurator)
  - [`@RequireJaxrsWhiteboard`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html#org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard)
  - [`@JSONRequired`](https://osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html#org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired)
  - [`@RequireCDIExtender`](https://osgi.org/specification/osgi.enterprise/7.0.0/service.cdi.html#org.osgi.service.cdi.annotations.RequireCDIExtender)
  - [`@RequireCDIImplementation`](https://osgi.org/specification/osgi.enterprise/7.0.0/service.cdi.html#org.osgi.service.cdi.annotations.RequireCDIImplementation)

Bnd Bundle Annotations:

* `@BundleCategory` – Sets the bundle category, existing categories are defined in an enum.
* `@BundleContributors` – Creates an OSGi header for contributors that maps to the Maven contributors element.
* `@BundleCopyright` – Sets the copyright header.
* `@BundleDevelopers` – Creates an OSGi header for developers that maps to the Maven developers element.
* `@BundleDocUrl` – Provides a documentation URL.
* `@BundleLicense` -  Creates entries in the `Bundle-License` header.
  * `@ASL_2_0`
  * `@BSD_2_Clause`
  * `@BSD_3_Clause`
  * `@CDDL_1_0`
  * `@CPL_1_0`
  * `@EPL_1_0`
  * `@GPL_2_0`
  * `@GPL_3_0`
  * `@LGPL_2_1`
  * `@MIT_1_0`
  * `@MPL_2_0`

* `@ServiceConsumer` - Generates requirements in support of the consumer side of the [Service Loader Mediator](https://osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html) specification.
* `@ServiceProvider` - Generates requirements and capabilities in support of the provider side of the [Service Loader Mediator](https://osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html) specification. Also generates `META-INF/service` descriptors.
