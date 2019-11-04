---
order: 330
title: JPMS Libraries
summary: Discusses how to use bnd to create JPMS libraries
layout: default
author: Raymond Augé
---

Java developers face a challenge today of building JPMS libraries, let alone when adding a secondary goal that those libraries be usable OSGi bundles. Accuracy and consistency of metadata can often become a problem reducing the time spent focusing on more productive aspect of the library.

A key **OSGi** innovation is the use of annotations to significantly reduce (in many cases to completely eliminate) the configuration needed to describe an OSGi bundle in build descriptors. See [Bundle Annotations](230-manifest-annotations.html).

## Creating JPMS Libraries with bnd

Just one of the additional benefits of using bnd is the ability to generate the `module-info.class`  along side all of the traditional OSGi metadata.

When applying the instruction `-jpms-module-info` bnd will collect any relevant information and using several heuristics automatically generates the `module-info.class`.

### Name, Version & Access

When calculating the **module name** the following is considered:

1. If the `-jpms-module-info` instruction contains a key, the key is used as the module name
   e.g. `-jpms-module-info: foo.module`, the module name is `foo.module`
2. If the `Automatic-Module-Name` header is found in the manifest, the value is used as the module name
3. Otherwise, the `Bundle-SymbolicName`  (as calculated) is used as the module name

When calculating the **module version**, the following is considered:

1. If the `-jpms-module-info` instruction contains a key having a `version` attribute, the `version` attribute value is used as the module version
   e.g. `-jpms-module-info: foo.module;version=1.2.3`, the module version is `1.2.3`
2. Otherwise, the `Bundle-Version`  (as calculated) is used as the module version

When calculating the **module access**, the following is considered:

1. If the `-jpms-module-info` instruction contains a key having an `access` attribute, the `access` attribute value is used as the module access
   e.g. `-jpms-module-info: foo.module;access=0x0020`, the module access is `0x0020` (OPEN)
   *Legal values are:*
   1.  `0x0020` (OPEN)
   2.  `0x1000` (SYNTHETIC)
   3.  `0x8000` (MANDATED)
2. If the header `Require-Capability` contains **any** capability in the `osgi.extender` namespace, the module access is **open**
3. Otherwise, module access is `0`

*Note that for the above rules, the earliest matching rule wins.*

### Requires

When calculating the module **requires** the following is considered:

1. If the `-jpms-module-info` instruction contains a key having a `modules` attribute, the `modules` attribute value is first split on commas (`,`) and each segment is added as a raw required module name
   e.g. `-jpms-module-info: foo.module;modules='java.desktop,java.logging'`, the modules `java.desktop`, and `java.logging` are added to module requires

2. In addition, if the `-jpms-module-info` instruction contains a key having a `ee` attribute, the `ee` attribute indicates the Java module name mapping table to use for Java SE packages using bnd's `aQute.bnd.build.model.EE` definitions which define a set of Java module name mapping tables keyed by `EE`.
   e.g. `-jpms-module-info: foo.module;ee=JavaSE_10_0`, bnd will use the Java module name mapping table for Java SE 10 when determining module name for a given Java SE package

   If no `ee` attribute is specified, bnd will use the Java module name mapping table for Java SE 11 when determining module name for a given Java SE package

3. If an imported package is associated with a module name, the module is added to module requires

   **Note:** Non-Java SE packages are associated with module names by indexing all packages on the classpath of the bnd `analyzer` where the providing jar's module's name is:

   - obtained from the jar's `module-info.class`
   - or, obtained from the jar's `Automatic-Module-Name`
   - or, calculated from the jar's filename by stripping off any version suffix (beginning with the first occurrence of a dash followed by a digit `-\d`)
   - otherwise, no module name is associated with the exported package

#### Requires - Access: Transitivity

Bnd will set the access to `transitive`  if _any_ package exported by the bundle has a `uses` constraint on a package of the required module.

#### Requires  - Access: Static

Bnd will set the access to `static` if the module is specified in the `-jpms-module-info` instruction and does not actual have any imports.

Bnd will set the access to `static` if _all_ the packages imported from the module are any combination of `resolution:=optional`, `resolution:=dynamic` or match the `Dynamic-ImportPackage` instruction.

#### Requires - Version

Bnd does not currently track a `require`'s version.

### Exports

Module **exports** will be mapped from all OSGi exported packages by default which can be managed easily with use of the [Bundle Annotation](230-manifest-annotations.html) `@org.osgi.annotation.bundle.Export` on `package-info.java`.

```java
@org.osgi.annotation.bundle.Export
package com.acme.foo;
```

**Targeted exports** (via the `exports .. to ..` clause) are supported with use of the `@aQute.bnd.annotation.jpms.ExportTo`. This annotation specifies the module name(s) to which a exported is targeted.

```java
@org.osgi.annotation.bundle.Export
@aQute.bnd.annotation.jpms.ExportTo("com.acme.bar")
package com.acme.foo;
```

**Note:** The `@ExportTo` annotation is only relevant in conjunction with the `@Export` annotation.

### Opens

Module **opens** are supported with use of the `@aQute.bnd.annotation.jpms.Open` annotation on `package-info.java`. This annotation optionally specifies the module name(s) to which the opens is targeted.

```java
@aQute.bnd.annotation.jpms.Open
package com.acme.foo;
```

### Uses

Module **uses** are supported transparently with use of the bnd [`@aQute.bnd.annotation.spi.ServiceConsumer` SPI Annotation](240-spi-annotations.html#serviceconsumer).

### Provides

Module **provides** are supported transparently with use of the bnd [`@aQute.bnd.annotation.spi.ServiceProvider` SPI Annotation](240-spi-annotations.html#serviceprovider).

### Main-Class

The module main class attribute is supported with use of the `@aQute.bnd.annotation.jpms.MainClass` annotation applied to the main class's Java type.

```java
@aQute.bnd.annotation.jpms.MainClass
public class Main {
	public static void main(String... args) {...}
}
```

### Java 8 Support

Bnd's `module-info.class` generation is supported when building with Java 8 or higher. (Yes! I did say Java 8.)

## Advanced Options

There are scenarios where the heuristics used by bnd don't give the desired result because the necessary information is not available or is incorrect.

The `-jpms-module-info-options` instruction provides some capabilities to help the developer handle these scenarios. The instruction uses the _package header syntax_ similar to many other bnd instructions. The keys of these instructions are module names and there are 4 available attributes. They are:

- **`substitute`** - If bnd generates a module name matching the value of this attribute it should be substituted with the key of the instruction.
  e.g. 
  
  ```properties
  -jpms-module-info-options: java.enterprise;substitute="geronimo-jcdi_2.0_spec"
  ```
  means that if bnd calculates the module name to be `geronimo-jcdi_2.0_spec` it should replace it with `java.enterprise` 
  
- **`ignore`** - If the attribute `ignore="true"` is found the require matching the key of the instruction will not be added.
  e.g. 
  
  ```properties
  -jpms-module-info-options: java.enterprise;ignore="true"
  ```

  means ignore the module `java.enterprise`
  
- **`static`** - If the attribute `static="true|false"` is found the access of the module matching the key of the instruction will be set to match.
  e.g. 
  
  ```properties
  -jpms-module-info-options: java.enterprise;static="true"
  ```

  means make the `require` for module `java.enterprise` `static`
  
- **`transitive`** - If the attribute `transitive="true|false"` is found the access of the module matching the key of the instruction will be set to match.
  e.g. 
  
  ```properties
  -jpms-module-info-options: java.enterprise;transitive="true"
  ```
  
  means make the `require` for module `java.enterprise` `transitive`

The following is an example with multiple attributes and instructions:

```properties
-jpms-module-info-options: \
    java.enterprise;substitute="geronimo-jcdi_2.0_spec";static=true;transitive=true,\
    java.management;ignore=true;
```

