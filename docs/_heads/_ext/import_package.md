---
layout: default
class: Header
title: Import-Package ::= import ( ',' import )*
summary: The Import-Package header declares the imported packages for this bundle.
---

# Import-Package

The `Import-Package` header lists the Java packages that the bundle requires from other bundles. By default, bnd will import all referred packages, but you can use patterns and negations to control which packages are imported.

Example:

```
Import-Package: !com.example.internal.*, *
```

This example imports all referred packages except those starting with `com.example.internal`. You can also add explicit imports for packages not directly referred to by your code.

bnd will attempt to find the exported version of imported packages and will use the exported version unless a specific version or range is specified. This header is important for managing dependencies between bundles.

The `Import-Package` header lists the packages that are required by the contained packages. The default for this header is `*`, resulting in importing all referred packages. This header therefore rarely has to be specified. However, in certain cases there is an unwanted import. The import is caused by code that the author knows can never be reached. This import can be removed by using a negating pattern. A pattern is inserted in the import as an extra import when it contains no wildcards and there is no referral to that package. This can be used to add an import statement for a package that is not referred to by your code but is still needed, for example, because the class is loaded by name.

For example:

    Import-Package: !org.apache.commons.log4j, com.acme.*,\
     com.foo.extra

During processing, bnd will attempt to find the exported version of imported packages. If no version or version range is specified on the import instruction, the exported version will then be used though the micro part and the qualifier are dropped. That is, when the exporter is `1.2.3.build123`, then the import version will be 1.2. If a specific version (range) is specified, this will override any found version. This default an be overridden with the [-consumerpolicy](../instructions/consumer_policy.html) or [-providerpolicy](../instructions/provider_policy.html) instruction (also see [Versioning](../chapters/170-versioning.html)).

If an explicit version is given, then `${@}` can be used to substitute the found version in a range. In those cases, the [range](../macros/range.html) macro can be very useful to calculate ranges and drop specific parts of the version. For example:

    Import-Package: org.osgi.framework;version="[1.3,2.0)"
    Import-Package: org.osgi.framework;version="${@}"
    Import-Package: org.osgi.framework;version="${range;[==,=+);${@}}"

You can reference the `Bundle-SymbolicName` and `Bundle-Version` of the exporter on the classpath by using the `${@bundlesymbolicname}` and `${@bundleversion}` values. In those cases, the [range](../macros/range.html) macro can be very useful to calculate ranges and drop specific parts of the bundle version. For example:

    Import-Package: org.eclipse.jdt.ui;bundle-symbolic-name="${@bundlesymbolicname}";\
     bundle-version="${range;[==,+);${@bundleversion}}"


Packages with directive `resolution:=dynamic` will be removed from `Import-Package` and added to the `DynamicImport-Package` header after being processed like any other `Import-Package` entry. For example:

    Import-Package: org.slf4j.*;resolution:=dynamic, *

## Conditional Resolution

Packages with directive `resolution:=conditional` provide a flexible way to handle imports based on the classpath configuration. This is particularly useful when wrapping third-party libraries where you want automatic handling of dependencies:

    Import-Package: *;resolution:=conditional

When a package is marked with `resolution:=conditional`, bnd will:

1. **Import normally** if the package is found on the classpath and comes from an OSGi bundle (has Export-Package metadata)
2. **Embed the package** if it's found on the classpath but comes from a non-OSGi jar (no OSGi metadata)
3. **Mark as optional** (`resolution:=optional`) if the package is not found on the classpath at all

This behavior is especially useful when:
- Wrapping existing JARs that mix OSGi and non-OSGi dependencies
- Creating flexible bundles that can adapt to different deployment scenarios
- Avoiding the need to manually specify which packages should be imported vs. embedded

Example use case: Wrapping a third-party library that depends on both OSGi frameworks (like org.osgi.framework) and plain Java libraries (like Apache Commons):

    Import-Package: *;resolution:=conditional
    Private-Package: com.thirdparty.*

In this case, OSGi packages will be imported with proper version ranges, while non-OSGi dependencies will be embedded directly into the bundle, and any optional dependencies not on the classpath will be marked as optional imports.

If an imported package uses mandatory attributes, then bnd will attempt to add those attributes to the import statement. However, in certain (bizarre!) cases this is not wanted. It is therefore possible to remove an attribute from the import clause. This is done with the `-remove-attribute` directive or by setting the value of an attribute to `!`. The parameter of the `-remove-attribute` directive is an instruction and can use the standard options with `!`, `*`, `?`, etc.

    Import-Package: org.eclipse.core.runtime;-remove-attribute:="common",*

Or

    Import-Package: org.eclipse.core.runtime;common=!,*

Directives that are not part of the OSGi specification will give a warning unless they are prefixed with `x-`.



<hr />
TODO Needs review - AI Generated content