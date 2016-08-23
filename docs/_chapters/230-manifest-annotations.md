---
order: 230
title: Manifest Annotations
layout: default
---

Entering manifest headers is error prone for a number of reasons since these headers are often complex, have different often hard to remember fields, and are singletons. Because they are singletons, there is only one place where they can be entered: the manifest. This is in contrast with the promoted component model where a component is stand-alone. Components should be cheap and easy to rename or move between bundles. This works fine for its Java imports but if you have an associated header in the manifest then it is easy to forget to also move its corresponding headers from the manifest. This can cause orphaned headers or missing headers either in the old bundle or the new bundle. The old bundle can miss the header because multiple components were depending on that header but it was mistakingly removed.

These headers are notoriously hard to write, it often takes several trials to get all the parts of the requirements correct. Also headers like Bundle-License are non trivial, especially if multiple licenses need to be entered. It is often hard to get the names right, especially if headers are not used very often.

One of the goals of bnd is to rely on Java and not escape to strings in Java to express semantics. The Java language is a rather steep cost from the point of view of coding but the type engine makes this cost-effective especially since it enables the IDE to assist the developers. However, when information then gets encoded in strings the advantages are voided and what is left is the cost.

## Manifest Annotations

To address these problems bnd has _manifest annotations_. A manifest annotation is applied to either a type or another annotation that is then customized. Using the type or the customized annotation on a type will then generate the corresponding header in the manifest. The resulting manifest header will only have  distinct clauses and not have errors. 

String fields in the manifest annotations are processed through bnd's macro processor. This macro processor provides access to all default, builder, project, and workspace macros.

The use of manifest annotations is two-fold. If applied to a type then they cause a clause in the manifest. If they are applied to another annotation then this annotation is a customized annotation. Only when a customized annotation is used to annotate a type then it will generate the corresponding clause in the manifest. The rationale is that this makes it possible to create an annotation for a subsystem. For example, it is possible to create an `@Angular` annotation that creates a requirement on a bundle with the javascript for Angular JS. Another example is an annotation `@ASL_2_0` that sets the Bundle-License header to the Apache Software License version 2.0.

All these annotations can be found in the biz.aQute.bndlib or biz.aQute.bnd.annotations bundles.

## Require & Provide Capability Manifest Annotations

Though the class files contain enough information to find the code dependencies, there are many dependencies that are indirect. OSGi _extenders_ are often a hard requirement to make a bundle function correctly but often have clients have no code dependency on the extender. For example, Declarative Services (DS) went out of its way to allow components to be Plain Old Java Objects (POJO). This is good, but the result is that resolving a closure of bundles starting from a client of DS would not drag in the Service Component Runtime (SCR), resulting in a satisfied but rather idle closure.

Meet the `@RequireCapability` and `@ProvideCapability` headers. 

These annotations can be applied to a type or any other annotation. If applied to a type then the annotation's requirement or capability will be added to the manifest. If applied to an annotation then this annotation is a customized annotation. Nothing happens until the annotated annotation is used. When this annotation is applied somewhere, bnd will automatically add the requirement or capability to the manifest.

### RequireCapability

The `@RequireCapability` annotation creates a clause in the Require-Capability manifest header. The annotation has the following fields (for more details consult the Javadoc):

* `value` – (String) Anything in the value field is appended after the calculated header.
* `ns` – (String) The namespace of the requirement
* `effective` – (String default `resolve`) The effective time
* `filter` – The filter directive. There is a handy macro [${frange;<version>][1] that turns a version into a filter expression for a filter range.  
* `resolution` – (`mandatory` | `optional`) The resolution of the requirement.

### ProvideCapability

The `@ProvideCapability` annotation creates a clause in the Provide-Capability manifest header. The annotation has the following fields (for more details consult the Javadoc):

* `value` – (String) Anything in the value field is appended after the calculated header.
* `ns` – (String) The namespace of the requirement
* `effective` – (String default `resolve`) The effective time
* `name` – (String) This creates a `<namespace>=<name>` attribute, the common convention to set the primary name attribute of a capability in most OSGi namespaces.
* `version` – (String) Set the version of the capability.
* `uses` – (String[]) Package names that are used by this capability and require the same class loader
* `mandatory` – (String[]) List of mandatory attributes

### Example

For example the following defines a capability for the OSGi enRoute Configurer.

	@RequireCapability(
	  ns        = “osgi.extender”, 
	  filter    = "(&(osgi.extender=osgi.enroute.configurer)${frange;1.2.3})",
	  effective = "active")

If this capability is applied to a type then the manifest will contain the corresponding requirement:

	@RequireCapability(
	  ns        = “osgi.extender”, 
	  filter    = "(&(osgi.extender=osgi.enroute.configurer)${frange;1.2.3})",
	  effective = "active")
	public class Peggy { ... }

This generates the following manifest headers (though the actual header can be larger since it can contain additional requirements):

	Require-Capability: osgi.extender;filter:='(&(osgi.extender=osgi.enroute.configurer`)
		(&(version>=1.2.3)(!(version>=2.0.0))))';effective:=active

It is also possible to create a customized annotation:

	@RequireCapability(
	  ns        = “osgi.extender”, 
	  filter    = "(&(osgi.extender=osgi.enroute.configurer)${frange;1.2.3})",
	  effective = "active")
	@Retention(RetentionPolicy)
	@Target( ElementType.TYPE )
	public @interface RequireConfigurer {}

This by itself does not create a clause in the manifest header. To make this happen we need to annotate a type:

	@RequireConfigurer
	public class Peggy { ... }

Now the same header will be generated.

## Bundle License

The `@BundleLicense` annotation creates entries in the Bundle-License header. The annotation has the following fields:

* `name` – The name of the license, should preferably the URI to the corresponding [Open Source Initiative][2] page about this license.
* `description` – A short description of the license.
* `url` – The URL to further information about the license

A number of licenses have gotten their own custom annotation:

* `ASL_2_0`
* `BSD_2_Clause`
* `BSD_3_Clause`
* `CDDL_1_0`
* `CPL_1_0`
* `EPL_1_0`
* `GPL_2_0`
* `GPL_3_0`
* `LGPL_2_1`
* `MIT_1_0`
* `MPL_2_0`

See the Javadoc for additional details.

### Example

	@ASL_2_0
	public class Peggy { ... }

	Bundle-License: \
		http://www.opensource.org/licenses/apache2.0.php; \
		description='Apache Software License 2.0';\
		link='http://www.apache.org/licenses/LICENSE-2.0.html'

## More Manifest Annotations

* `@BundleCopyright` – Sets the copyright header.
* `@BundleCategory` – Sets the bundle category, existing categories are defined in an enum.
* `@BundleContributors` – Creates an OSGi header for contributors that maps to the Maven contributors element.
* `@BundleDevelopers` – Creates an OSGi header for developers that maps to the Maven developers element.
* `@BundleDocUrl` – Provides a documentation URL.


[1]: /macros/frange.html
[2]: http://opensource.org