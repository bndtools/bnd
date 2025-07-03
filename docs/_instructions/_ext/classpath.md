---
layout: default
class: Analyzer
title: -classpath  FILE (',' FILE) *
summary: Specify additional file based entries (either directories or JAR files) to add to the used classpath. 
---
The `-classpath` instruction adds class path entries to a bnd file's processing. It contains direct references to JARs through file paths or URLs. Entries on the class path are made available as imports and can be used as private or exported packages.

	-classpath: jar/foo.jar, jar/bar.jar

If you need to get all JARs in a directory you could use the `${lsa}` macro:

	-classpath: ${lsa;jar/*}
	
This instruction should only be used when bnd is used in stand alone mode. In project mode (when you have a workspace), the `-buildpath` defines the actual class path. However, even in project mode any `-classpath` entries are added to the actual class path. 
