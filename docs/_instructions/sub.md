---
layout: default
title: -sub FILE-SPEC ( ',' FILE-SPEC )*
class: Builder
summary: |
   Enable sub-bundles to build a set of .bnd files that use bnd.bnd file as a basis. The list of bnd files can be specified with wildcards.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-sub=com.acme.*.bnd`

- Pattern: `.*`

<!-- Manual content from: ext/sub.md --><br /><br />

You can enable **sub-bundles** with the `-sub` instruction, to build a set of `.bnd` files that use the `bnd.bnd` file as a basis. The list of sub-`.bnd` files can be specified with wildcards.

## Example 

Assume a bundle `com.example` with a main `bnd.bnd` file:

`-sub *.bnd`

And besides the following files:

- `sub.bundle1.bnd`
- `sub.bundle2.bnd`

This will process every other `.bnd` file as a `sub-bundle` and will create a `.jar` file for it:

- `generated/com.example.sub.bundle1.jar`
- `generated/com.example.sub.bundle2.jar`

The `Bundle Symbolic Name` for each sub bundle will be the name of the `project` + `.` + `file name prefix of the sub bundle`. 

Sub bundles are mostly treated as independent projects. They are part of the repositories and release process.

The content of the sub `.bnd` files is as usual (see also [Generating JARs](/chapters/160-jars.html)).


## -buildpath and -classpath with sub-bundles

The [`-buildpath`](/instructions/buildpath.html) is a project instruction. This means that it must be placed in the main `bnd.bnd` file, not in the sub-`.bnd` file. A project can only have a single `-buildpath`.

Thus, in the sub bundle `.bnd`-files you have to use [`-classpath`](https://bnd.bndtools.org/instructions/classpath.html) instead. 

**Example:**

```
-classpath: \
	${repo;org.apache.poi:poi;latest},\
```

## Use cases

### Gogo Commands

A case where this approach is useful is, when you provide an implementation and want to separate the Gogo commands from the main code. In that case you create a **main provider** bundle and a **Gogo bundle**.

In most cases it is best to have one project generate one bundle. However, sometimes it creates a lot of overhead and you’d like to be able to provide multiple bundles in a project since they share so much information.

### Wrapping JAR files

Another use case is when you need to [wrap `.jar` files](/chapters/390-wrapping.html), e.g. to convert non-OSGi bundles into OSGi-Bundles by adding meta-data to the Manifest. 
A practise to do so could be to have a single Wrapper-bundle where you do all your wrapping of external dependencies. The advantage is, that you don't clutter up your workspace with lots of tiny Bundle-wrapping projects. With sub-bundles you can keep all your wrapped jars in one single project. 
