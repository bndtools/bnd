---
layout: default
title: xref
summary: |
   Show a cross references for all classes in a set of jars.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   xref [options]  <<jar path>> <[...]>

#### Options: 
- `[ -c --classes ]` Show classes instead of packages
- `[ -d --destination <string>* ]` Match destination types
- `[ -f --from ]` Show references from other classes/packages (<)
- `[ -j --java ]` Include java.* packages
- `[ -m --match <string>* ]` Filter for class names, a globbing expression
- `[ -n --nested ]` Analyze nested JARs referenced via Bundle-ClassPath
- `[ -r --referrredTo <string> ]` Output list of package/class names that have been referred to
- `[ -s --source <string>* ]` Match source types
- `[ -t --to ]` Show references to other classes/packages (>)

<!-- Manual content from: ext/xref.md --><br /><br />

## Examples
   
	   biz.aQute.bnd (master)$ bnd xref generated/*.jar
	                              aQute.bnd.annotation > 
	                    aQute.bnd.annotation.component > 
	                      aQute.bnd.annotation.headers > 
	                     aQute.bnd.annotation.licenses > 
	                     aQute.bnd.annotation.metatype > 
	                                     aQute.bnd.ant > aQute.service.reporter
	                                                     org.apache.tools.ant
	                                                     aQute.libg.reporter
	                                                     org.apache.tools.ant.taskdefs
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.build
	                                                     aQute.libg.qtokens
	                                                     org.apache.tools.ant.types
	                                                     aQute.bnd.osgi.eclipse
	                                                     aQute.bnd.service.progress
	                                                     aQute.bnd.service
	                                                     aQute.bnd.version
	                                                     aQute.bnd.build.model
	                                                     aQute.bnd.build.model.clauses
	                                   aQute.bnd.build > aQute.service.reporter
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.service
	                                                     aQute.libg.command
	                                                     aQute.libg.sed
	                                                     aQute.bnd.version
	                                                     aQute.bnd.service.action
	                                                     aQute.bnd.header
	                                                     aQute.lib.io
	                                                     aQute.libg.reporter
	                                                     aQute.bnd.osgi.eclipse
	                                                     aQute.bnd.help
	                                                     aQute.lib.strings
	                                                     aQute.libg.generics
	                                                     aQute.bnd.maven.support
	                                                     aQute.libg.glob
	                                                     aQute.lib.converter
	                                                     aQute.lib.collections
	                                                     aQute.bnd.differ
	                                                     aQute.bnd.service.diff
	                                                     aQute.bnd.service.repository
	                                                     aQute.lib.deployer
	                                                     javax.naming
	                                                     aQute.lib.hex
	                                                     aQute.bnd.resource.repository
	                                                     aQute.bnd.url
	                                                     aQute.lib.settings
	                                                     aQute.bnd.service.url
	                                                     aQute.bnd.service.extension
	                             aQute.bnd.build.model > aQute.bnd.build.model.conversions
	                                                     aQute.libg.tuple
	                                                     aQute.bnd.build.model.clauses
	                                                     aQute.bnd.header
	                                                     aQute.bnd.properties
	                                                     aQute.bnd.build
	                                                     aQute.bnd.version
	                                                     aQute.lib.io
	                                                     org.osgi.resource
	                                                     aQute.bnd.osgi
	                     aQute.bnd.build.model.clauses > aQute.bnd.header
	                 aQute.bnd.build.model.conversions > aQute.bnd.header
	                                                     aQute.libg.tuple
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.build.model
	                                                     aQute.bnd.build.model.clauses
	                                                     org.osgi.resource
	                                                     aQute.bnd.osgi.resource
	                                                     aQute.libg.qtokens
	                           aQute.bnd.compatibility > aQute.bnd.osgi
	                               aQute.bnd.component > aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.lib.collections
	                                                     org.osgi.service.component.annotations
	                                                     aQute.bnd.version
	                                                     aQute.bnd.component.error
	                                                     aQute.lib.tag
	                                                     aQute.bnd.header
	                                                     aQute.bnd.service
	                         aQute.bnd.component.error > 
	                                  aQute.bnd.differ > aQute.bnd.header
	                                                     aQute.bnd.service.diff
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.version
	                                                     aQute.service.reporter
	                                                     aQute.libg.generics
	                                                     aQute.libg.cryptography
	                                                     aQute.lib.hex
	                                                     aQute.lib.io
	                                                     aQute.lib.collections
	                                                     aQute.bnd.annotation
	                                                     aQute.bnd.service
	                        aQute.bnd.enroute.commands > aQute.lib.getopt
	                                                     aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.bnd.main
	                                                     aQute.bnd.build
	                                                     aQute.lib.io
	                                aQute.bnd.filerepo > aQute.bnd.version
	                                  aQute.bnd.header > aQute.bnd.version
	                                                     aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.libg.generics
	                                                     aQute.libg.qtokens
	                                                     aQute.lib.collections
	                                    aQute.bnd.help > aQute.bnd.osgi
	   

## Nested JARs

By default, `xref` only analyzes classes directly contained in the provided JAR files. Many OSGi bundles contain nested JARs that are referenced via the `Bundle-ClassPath` manifest header. To analyze these nested JARs as well, use the `--nested` option:

	   bnd xref --nested mybundle.jar

This will:
1. Parse the `Bundle-ClassPath` manifest header
2. Extract and analyze any embedded JAR files
3. Analyze directories referenced in the Bundle-ClassPath
4. Include cross-references from all sources in the output

For example, given a bundle with the following structure:

```
mybundle.jar
├── META-INF/MANIFEST.MF (Bundle-ClassPath: .,lib/internal.jar)
├── com/example/Main.class
└── lib/internal.jar
    └── com/example/internal/Helper.class
```

Without `--nested`, only `com.example.Main` would be analyzed. With `--nested`, both `com.example.Main` and `com.example.internal.Helper` classes are included in the cross-reference analysis.
