---
order: 160
title: Generating JARs
layout: default
---

This is about generating OSGi JARs - one of the main tasks of bnd.

This is a simple example of [wrapping a jar with bnd](/chapters/390-wrapping.html) (wrapping means: taking a non-OSGi jar and use `bnd` to add OSGi meta data to its MANIFEST.MF to get a proper OSGi `.jar`). 

The basic idea is to create a recipe (a `.bnd` file) that collects the different resources in the right way to create the new output `.jar` including the OSGi meta data in `MANIFEST.MF`.

For example, you want to wrap the WebSocket server from https://github.com/TooTallNate/Java-WebSocket/releases/tag/Java-WebSocket-1.3.4. Download the [binary](https://github.com/TooTallNate/Java-WebSocket/releases/download/Java-WebSocket-1.3.4/Java-WebSocket-1.3.4.jar) (Java-WebSocket-1.3.4.jar) and the [sources](https://github.com/TooTallNate/Java-WebSocket/releases/download/Java-WebSocket-1.3.4/Java-WebSocket-1.3.4-sources.jar) in Java-WebSocket-1.3.4-sources.jar. 

## Project

Once you have these files, create a folder e.g. `mkdir wrappers` and add `bnd.bnd` file in the project to create the `org.websocket.jar` bundle.

    # bnd.bnd
    # Wrapped version of Github project TooTallNate/Java-WebSocket
    Bundle-SymbolicName: org.websocket
    Bundle-DocURL: https://github.com/TooTallNate/Java-WebSocket
    Bundle-License: https://github.com/TooTallNate/Java-WebSocket/blob/8ef67b46ecc927d5521849dcc2d85d10f9789c20/LICENSE
    Bundle-Description: This repository contains a barebones \
     WebSocket server and client implementation written \
     in 100% Java. The underlying classes are implemented \
     using the Java ServerSocketChannel and SocketChannel \
     classes, which allows for a non-blocking event-driven model \
     (similar to the WebSocket API for web browsers). \
     Implemented WebSocket protocol versions are: Hixie 75, \
     Hixie 76, Hybi 10, and Hybi 17

    # version taken from the downloaded file above
    Bundle-Version: 1.3.4

    -includeresource: @Java-WebSocket-1.3.4.jar, OSGI-OPT/src=@Java-WebSocket-1.3.4-sources.jar
    -exportcontents: org.java_websocket

Either use the [bndtools Eclipse plugin](https://bndtools.org/) or the [bnd CLI](/chapters/400-commands.html) to process the `bnd.bnd` file and let bnd create the .jar. In case of bnd CLI the command is:

    bnd bnd.bnd


## Manifest

Applying this recipe above gives the following `META-INF/MANIFEST.MF` in a JAR named `/generated/org.websocket.jar`:

    Manifest-Version: 1.0
    Bnd-LastModified: 1338190175969
    Bundle-Description: This repository contains a barebones WebSocket serve
     r and client implementation written in 100% Java. The underlying classe
     s are implemented using the Java ServerSocketChannel and SocketChannel 
     classes, which allows for a non-blocking event-driven model (similar to
     the WebSocket API for web browsers). Implemented WebSocket protocol ve
     rsions are: Hixie 75, Hixie 76, Hybi 10, and Hybi 17
    Bundle-DocURL: https://github.com/TooTallNate/Java-WebSocket
    Bundle-License: https://github.com/TooTallNate/Java-WebSocket/blob/8ef67
     b46ecc927d5521849dcc2d85d10f9789c20/LICENSE
    Bundle-ManifestVersion: 2
    Bundle-Name: org.websocket
    Bundle-SymbolicName: org.websocket
    Bundle-Version: 1.3.4
    Export-Package: org.java_websocket;version="1.3.4";uses:="javax.net.ss
    l"
    Import-Package: javax.net.ssl
    Private-Package: org.java_websocket.client,org.java_websocket.drafts,o
    rg.java_websocket.exceptions,org.java_websocket.framing,org.java_webs
    ocket.handshake,org.java_websocket.server,org.java_websocket.util
    Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.5))"



You notice that the `.bnd` file and the `MANIFEST` look similar but the bnd instructions caused bnd to generate headers like `Export-Package` and `Import-Package`. bnd did that by analysing the `.class` files inside `Java-WebSocket-1.3.4.jar` (e.g. by looking at all the `import` statements classes, methods, method parameters and return types).

Note: The documentation headers like `Bundle-Description` are optional but very important, just spent the minute to document them since you'll be grateful later.

If the target project does not have a version, makeup a version and maintain it. Notice that in general the recipe will only be used once for each version, it is normally not used in continuous integration builds. However, you normally use it to convert the next version of the project. Crisp versioning is important.

The [`-includeresource`](/instructions/includeresource.html) statement unrolls the jars we downloaded in the root of the JAR and in `OSGI-OPT`. Since the source code is in the `src` directory in the  `WebSocket-src.zip` file, we put it in the new JAR under `OSGI-OPT/src`. This convention is supported by all IDEs to give you direct access to the bundle's source code. Since the binary and the source are kept together, you always have the correct source code available, and usually automatically. It is so convenient that once you're used to this it is hard to imagine a life without source code.

The binaries and sources are not in the final jar but bnd does not yet know what needs to be exported. This can be indicated with the [`-exportcontents`](/instructions/exportcontents.html) instruction. It has the same syntax as [`Export-Package`](/heads/export_package.html) but does not copy from the classpath, it only applies the instruction to the content of the final JAR.

## OSGi Header Attribute and Directive Ordering

When bnd processes OSGi manifest headers, it automatically ensures consistent ordering of attributes and directives within header clauses. This ordering is applied to OSGi syntax headers such as `Export-Package`, `Import-Package`, `Require-Capability`, `Provide-Capability`, and others.

### Ordering Rules

Bnd applies the following ordering rules to attributes and directives within OSGi header clauses:

1. **Attributes come before directives** - All attributes (keys without a trailing colon) are placed before directives (keys with a trailing colon)
2. **Alphabetical sorting within groups** - Within the attribute group and directive group, keys are sorted alphabetically (case-insensitive)

### Example

Given an input header like:
```
Export-Package: com.example.api;uses:="com.example.internal";version=1.0.0;mandatory:="version";provider=acme
```

Bnd will reorder it to:
```
Export-Package: com.example.api;provider=acme;version=1.0.0;mandatory:="version";uses:="com.example.internal"
```

Notice how:
- Attributes (`provider`, `version`) come first, sorted alphabetically
- Directives (`mandatory:`, `uses:`) come second, sorted alphabetically

### Benefits

This consistent ordering provides several benefits:

- **Reproducible builds** - The same input always produces the same output, regardless of the original order (see also [-reproducible](/instructions/reproducible.html))
- **Easier comparison** - Manifest files can be compared more easily when attributes and directives are consistently ordered
- **Better readability** - Consistent ordering makes manifest headers easier to read and understand

### Affected Headers

This ordering is applied to all headers defined in `Constants.OSGI_SYNTAX_HEADERS`, which includes:
- `Export-Package`
- `Import-Package`
- `Require-Capability`
- `Provide-Capability`
- `Bundle-SymbolicName`
- `Fragment-Host`
- And other OSGi-defined headers that use the clause syntax

## Extra entries on the Classpath
One of the great features of bnd is to use export version from other versions to generate the import ranges. This feature requires that the other JARs are on the classpath. In bndtools you can use the -buildpath. However, you always add entries on the class path per bnd descriptor with the [-classpath](/instructions/classpath.html) instruction:

    -classpath: dependency.jar, other.jar

# Wrapper project


The easiest way to build these wrappers is to create a project in bndtools called `wrappers` and create a main `bnd.bnd` with the following content:


```
-sub: *.bnd
```

The create a `bnd.bnd` descriptor for each wrapper you need to build. e.g. 

- `websocket.bnd`
- `someotherlib.bnd`

When the main `bnd.bnd` is processed bnd creates a jar for each sub- .bnd file.
E.g. 

```
bnd bnd.bnd
```

creates a `/generated/websocket.jar` and `/generated/someotherlib.jar`.


# All headers

All headers are listed on the [Headers index](/chapters/800-headers.html)