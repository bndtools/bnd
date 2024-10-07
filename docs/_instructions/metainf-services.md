---
layout: default
class: Analyzer
title: -metainf-services
summary:  Controls how META-INF/services files are processed.
---

The `-metainf-services` instruction tells **bnd** how files in the `META-INF/services` should be processed.
See the chapter about [META-INF/services Annotations](/chapters/230-manifest-annotations.html#meta-infservices-annotations) how to apply Bundle annotations to the files in the `META-INF/services`.

This instruction can have the following values:

- `auto` (default if not set): auto-register services without textual annotations. That means that bnd auto-generates a `aQute.bnd.annotation.spi.ServiceProvider` annotation without attributes 'under the hood' if an Implementation doesn't have one. This is useful if you just want to have bnd generate the `Provide-Capability` headers for `osgi.serviceloader`. Additionally 'auto' behaves like 'annotation'.
- `annotation`: Scan files and only process textual annotations in comments of META-INF/services files.
- `none`: disable processing of files in `META-INF/services`

## Example

Assume we want to wrap a non-OSGi library.
Create `bnd.bnd` file and a library having a `META-INF/services` folder e.g. [groovy-3.0.22.jar](https://mvnrepository.com/artifact/org.codehaus.groovy/groovy/3.0.22) in a local `lib` folder.
We will use `-includeresource: @lib/groovy-3.0.22.jar` to [unroll the jar](/instructions/includeresource.html#rolling). 

```
# bnd.bnd
-includeresource: @lib/groovy-3.0.22.jar
```

This uses the default `-metainf-services: auto` and creates a new jar with the following MANIFEST.MF headers from the `META-INF/services` folder:

```
# MANIFEST.MF
Provide-Capability                      osgi.service;objectClass:List<String>="org.codehaus.groovy.transform.ASTTransformation";effective:=active
                                        osgi.serviceloader;osgi.serviceloader="org.codehaus.groovy.transform.ASTTransformation";register:="groovy.grape.GrabAnnotationTransformation"
Require-Capability                      osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
                                        osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))"
```

Because the default `-metainf-services: auto` is used, it instructs bnd to process textual annotations and also auto-generate a `@ServiceProvider` annotation annotation under the hood for services without annotations. 
To prevent the latter (auto-generation) use the default `-metainf-services: annotation` or use `-metainf-services: none`.

[source](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib/src/aQute/bnd/osgi/metainf/MetaInfServiceParser.java)