---
layout: default
class: Analyzer
title: -metainf-services
summary:  Controls how META-INF/services files are processed.
---

The `-metainf-services` instruction tells **bnd** how files in the `META-INF/services` should be processed.
See the chapter about [META-INF/services Annotations](/chapters/230-manifest-annotations.html#meta-infservices-annotations) how to apply Bundle annotations to the files in the `META-INF/services`.

This instruction can have the following values:

- `annotation` (default if not set): Scan META-INF/services files and only process those files which contain at least one textual annotations.
- `auto`: The convenience strategy. Scans `META-INF/services` files and auto-registers implementations in services files which do not have a single textual annotation. The latter means that bnd behaves as if there was a `aQute.bnd.annotation.spi.ServiceProvider` annotation present on each implementation. This is useful if you just want to have bnd generate the `Provide-Capability` headers for `osgi.serviceloader`. Additionally `auto` behaves like `annotation` and would process all other files with textual annotations.
- `none`: skip processing of files in `META-INF/services` completely

## Example of auto-registration

Assume we want to wrap a non-OSGi library containing `META-INF/services`.
Create `bnd.bnd` file and put a library having a `META-INF/services` folder e.g. [groovy-3.0.22.jar](https://mvnrepository.com/artifact/org.codehaus.groovy/groovy/3.0.22) in a local `lib` folder.
We will use `-includeresource: @lib/groovy-3.0.22.jar` to [unroll the jar](/instructions/includeresource.html#rolling). 

```
# bnd.bnd
-includeresource: @lib/groovy-3.0.22.jar
-metainf-services: auto
```

This creates a new jar with the following MANIFEST.MF headers from the `META-INF/services` folder:

```
# MANIFEST.MF
Provide-Capability                      osgi.service;objectClass:List<String>="org.codehaus.groovy.transform.ASTTransformation";effective:=active
                                        osgi.serviceloader;osgi.serviceloader="org.codehaus.groovy.transform.ASTTransformation";register:="groovy.grape.GrabAnnotationTransformation"
Require-Capability                      osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
                                        osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))";resolution:=optional
```

Because `-metainf-services: auto` is used, it instructs bnd to auto-generate a `@ServiceProvider` annotation under the hood for services without annotations. 
To prevent the latter (auto-generation) use the default `-metainf-services: annotation` (to process only textual annotations) or use `-metainf-services: none` to skip processing of `META-INF/services` files completely.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/osgi/metainf/MetaInfServiceParser.java)