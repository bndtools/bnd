---
layout: default
class: Analyzer
title: -metainf-services
summary:  Controls how META-INF/services files are processed.
---

The `-metainf-services` instruction tells **bnd** how files in the `META-INF/services` should be processed.
See the chapter about [META-INF/services Annotations](/chapters/230-manifest-annotations.html#meta-infservices-annotations) how to apply Bundle annotations to the files in the `META-INF/services`.

This instruction can have the following values:

- `annotation` (default if not set): Scan files and only process annotations.
- `auto`: process annotations and special treatment and auto-register services without annotations. That means that bnd auto-generates a `aQute.bnd.annotation.spi.ServiceProvider` annotation without attributes if an Implementation doesn't have one. This is useful if you just want to have bnd generate the `Provide-Capability` headers for `osgi.serviceloader`.
- `none`: disable processing of files in `META-INF/services`

## Example

Assume we want to wrap a non-OSGi library.
Create `bnd.bnd` file and a library having a `META-INF/services` folder e.g. [groovy-3.0.22.jar](https://mvnrepository.com/artifact/org.codehaus.groovy/groovy/3.0.22) in a local `lib` folder.
We will use `-includeresource: @lib/groovy-3.0.22.jar` to [unroll the jar](/instructions/includeresource.html#rolling). 

```
# bnd.bnd
-buildpath: biz.aQute.bnd.annotation;version='7.0'
-includeresource: @lib/groovy-3.0.22.jar
-metainf-services: auto
```

This creates a new jar with the following MANIFEST.MF headers from the `META-INF/services` folder:

```
# MANIFEST.MF
Provide-Capability                      osgi.service;objectClass:List<String>="org.codehaus.groovy.transform.ASTTransformation";effective:=active
                                        osgi.serviceloader;osgi.serviceloader="org.codehaus.groovy.transform.ASTTransformation";register:="groovy.grape.GrabAnnotationTransformation"
Require-Capability                      osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
                                        osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))"
```

`-metainf-services: auto` causes bnd to process annotations and also auto-generate an annotation for services without annotations. 
To prevent the latter (auto-generation) use the default `-metainf-services: annotation` or remove the instruction completely.

**Note:** Make sure `biz.aQute.bnd.annotation` is on the classpath / buildpath which contains `aQute.bnd.annotation.spi.ServiceProvider` annotation.



[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/osgi/metainf/MetaInfServiceParser.java)