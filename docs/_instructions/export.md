---
layout: default
class: Project
title: -export PATH ( ';' PARAMETER )* ( ',' PATH ( ';' PARAMETER )* )*
summary: Turns a bndrun file into its deployable format 
---

The `-export` instruction can be used to export a `bndrun` or `bnd` file to an executable JAR during the build process. In contrast to the export functions, this `-export` instruction is supported in all drivers, including Eclipse. It can be used to continously build executable JARs or other supported exporters. It can only be used in the top level bnd.bnd file.

The format of the `-export` instruction is:

    -export     ::= filespec ( ',' filespec )*
    filespec    ::= path (';' PARAMATER )*

The `path` is either a relative path in the project directory or it is a wildcard specification using globs. All files in the project directory are selected. In general, these should be bnd or bndrun files.

The following attributes are architected:

* `type` – The type specifies the exporter type. The default type is `bnd.executablejar.pack`.
* `name` – Overrides the default name of the output file. It should contain the extension. The file should not have path segments, it will be placed in the target directory. Without a name, the exporter defines the file name since the extensions can differ depending on the exporter.
* `bsn` – Set the bundle symbolic name of the resulting JAR file if possible
* `version` – Set the version of the resulting JAR file if possible
* other – Any remaining properties are added to the properties of the run file. For example, `-profile` can be used to select a specific property profile.

The `-exporttype` can be used to set some default attributes for a specific type.

## Exporters

The Exporters use a plugin mechanism and therefore the list is not closed. The following exporters are supported by bnd out of the box:

* `bnd.executablejar.pack` – Exports a JAR file using the launcher from the `-runpath` or the default if no launcher is specified.
* `bnd.executablejar` – Similar to the previous but does not support profiles, has no automatic bsn assigned, and entries are not signed. The reason there are two types for the more or less the same format with subtle differences is for backward compatibility.
* `bnd.runbundles` – A JAR with the runbundles
* `osgi.subsystem.application` – Export into an application subsystem
* `osgi.subsystem.feature` – Export into a feature subsystem 
* `osgi.subsystem.composite` – Export into a composite subsystem

## Example

For example:

    -export: \
        foo.bndrun; \
            -profile=debug; \
            -runkeep=true; \
            name = "foo.jar, \
        bar.bndrun

## Backward Compatibility

If the  `filespec` clause does not set the `type` nor the `name` then it is assumed the backward compatible mode is required. This will set the output name to the file name with a `.jar` extension and it will set the bundle symbolic name.
