---
layout: default
title: -export-apiguardian PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*
class: Project
summary: |
   Exports the given packages where the the `@API` annotation is found on contained classes.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-export-apiguardian=!*impl*,*;version=3.0`

- Pattern: `.*`

<!-- Manual content from: ext/export_apiguardian.md --><br /><br />

**[API Guardian](https://github.com/apiguardian-team/apiguardian)** is a

> Library that provides the `@API` annotation that is used to  annotate public types, methods, constructors, and fields within a  framework or application in order to publish their status and level of  stability and to indicate how they are intended to be used by consumers  of the API.

The heuristic used by **bnd** to determine which packages to export is as follows:

-  the instruction `-export-apiguardian` is required (i.e. opt-in is required). The value of the instruction is a package specification like most other [bnd instructions](/chapters/820-instructions.html). The minimal configuration is `-export-apiguardian: *` which instructs bnd to scan all classes in all packages with the plugin
- the plugin will export packages which have any class containing the `@API` annotation
- package exports will be marked with the attribute `status` using the highest value of `org.apiguardian.api.API.Status` (in ordinal order) found on `@API` annotations in the package
- package exports will be marked with the directive `mandatory:=status` for any package whose highest value of `org.apiguardian.api.API.Status`  found was `INTERNAL` . This implies that the package can only be imported by marking the import with the attribute `status=INTERNAL` 

#### Examples

Export all packages annotated with `@API` using version `1.2.3`:

```properties
-export-apiguardian: *;version="1.2.3"
```

Export only packages annotated with `@API` named `com.acme.foo.*` but not `com.acme.foo.bar.*`

```properties
-export-apiguardian:\
	!com.acme.foo.bar.*,\
	com.acme.foo.*
```
