___
___
# Wrapping

This is a simple example of wrapping a jar with bnd. The basic idea is to create a recipe (a .bnd file) that collects the different resources in the right way.

For example, you want to wrap the WebSocket server from https://github.com/TooTallNate/Java-WebSocket. Download the binary and the sources in Websocket.jar, Websocket-src.zip. Once you have these files, the following code will create the org.websocket bundle

  # Wrapped version of Github project TooTallNate/Java-WebSocket
  Bundle-SymbolicName: org.websocket
  Bundle-DocURL: https://github.com/TooTallNate/Java-WebSocket
  Bundle-License: https://github.com/TooTallNate/Java-WebSocket/
          blob/8ef67b46ecc927d5521849dcc2d85d10f9789c20/LICENSE
  Bundle-Description: This repository contains a barebones \ 
   WebSocket server and client implementation written \ 
   in 100% Java. The underlying classes are implemented \ 
   using the Java ServerSocketChannel and SocketChannel \ 
   classes, which allows for a non-blocking event-driven model \ 
   (similar to the WebSocket API for web browsers). \ 
   Implemented WebSocket protocol versions are: Hixie 75, \ 
   Hixie 76, Hybi 10, and Hybi 17

  # websocket does not define a version yet :-(
  Bundle-Version: 1.0.2

  Include-Resource: @jar/WebSocket.jar, OSGI-OPT/src=@jar/WebSocket-src.zip
  -exportcontents: org.java_websocket


The documentation headers are optional but very important, just spent the minute to document them since you'll be grateful later.

If the target project does not have a version, makeup a version and maintain it. Notice that in general the recipe will only be used once for each version, it is normally not used in continuous integration builds. However, you normally use it to convert the next version of the project. Crisp versioning is important.

The `Include-Resource` statement unrolls the jars we downloaded in the root of the JAR and in `OSGI-OPT`. Since the source code is in the `src` directory in the  `WebSocket-src.zip` file, we put it in the new JAR under `OSGI-OPT/src`. This convention is supported by all IDEs to give you direct access to the bundle's source code. Since the binary and the source are kept together, you always have the correct source code available, and usually automatically. It is so convenient that once you're used to this it is hard to imagine a life without source code.

The binaries and sources are not in the final jar but bnd does not yet know what needs to be exported. This can be indicated with the `-exportcontents` instruction. It has the same syntax as `Export-Package` but does not copy from the classpath, it only applies the instruction to the content of the final JAR.

## Project
The easiest way to build these wrappers is to create a project in bndtools called wrappers and create a bnd descriptor for each one. They are then automatically build (look in generated) and you get a lot of help editing the bnd files.

You can also make an ant file but this is not described here (volunteer?). The other possibility is to use bnd from the [[CommandLine]]. In this case the command is:

  bnd websocket.bnd

## Manifest
Applying this recipe gives the following manifest in a JAR named `org.websocket.jar`:

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
  Bundle-Version: 1.0.2
  Created-By: 1.6.0_27 (Apple Inc.)
  Export-Package: org.java_websocket;version="1.0.2"
  Include-Resource: @jar/WebSocket.jar, OSGI-OPT/src=@jar/WebSocket-src.zip
  Private-Package: org.java_websocket.handshake,org.java_websocket.drafts,
   org.java_websocket.exceptions,org.java_websocket.util,org.java_websocke
   t.framing
  Tool: Bnd-1.51.0

## Extra entries on the Classpath
One of the great features of bnd is to use export version from other versions to generate the import ranges. This feature requires that the other JARs are on the classpath. In bndtools you can use the -buildpath. However, you always add entries on the class path per bnd descriptor with the -classpath instruction:

  -classpath: dependency.jar, other.jar


