---
layout: default
class: Builder
title: -namesection RESOURCE-SPEC ( ',' RESOURCE-SPEC ) *   
summary:  Create a name section (second part of manifest) with optional property expansion and addition of custom attributes. Patterns not ending with \"/\" target resources. Those ending with \"/\" target packages. 
---

Create a name section (second part of manifest) with optional property expansion and addition of custom attributes.

### Matching
The key of the `-namesection` instruction is an _ant style_ glob. And there are two target groups for matching:

* **resources** - the pattern not ending with `/` or is an exact match for a resource path
* **packages** - the pattern ends with `/` or is an exact match for a package path 

#### Custom attributes

The goal of named sections is to provide attributes over a specific subset of resources and paths in the jar file. Attributes are specified using the same syntax used elsewhere (such as package attributes). Attributes can contain properties and macros for expansion and replacement.

Each attribute is processed by bnd and the matching value is passed using the `@` property.

#### Resources
Resources are targeted by using a glob pattern not ending with `/`.

For example, the following instruction sets the content type attribute for `png` files:

```properties
-namesection: com/foo/*.png; Content-Type=image/png
```

This produces a result like the following:
```properties
Name: org/foo/icon_12x12.png
Content-Type: image/png

Name: org/foo/icon_48x48.png
Content-Type: image/png
```

#### Packages
Packages are targeted by using a glob pattern that ends with `/`.

For example, to produce a Java [Package Version Information](https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html) section use an instruction like this one:
```properties
-namesection: jakarta/annotation/*/;\
	Specification-Title=Jakarta Annotations;\
	Specification-Version=${annotation.spec.version};\
	Specification-Vendor=Eclipse Foundation;\
	Implementation-Title=jakarta.annotation;\
	Implementation-Version=${annotation.spec.version}.${annotation.revision};\
	Implementation-Vendor=Apache Software Foundation
```

This produces a result like the following:
```properties
Name: jakarta/annotation/
Implementation-Title: jakarta.annotation
Implementation-Vendor: Apache Software Foundation
Implementation-Version: 2.0.0-M1
Specification-Title: Jakarta Annotations
Specification-Vendor: Eclipse Foundation
Specification-Version: 2.0
```

