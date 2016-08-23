---
layout: default
class: Project
title: -snapshot STRING 
summary: String to substitute for "SNAPSHOT" in the bundle version's qualifier
---

When the bundle version's qualifier equals "SNAPSHOT" or ends with "-SNAPSHOT", the STRING
value of the `-snapshot` instruction is substituted for "SNAPSHOT". The STRING value of
the empty string will remove the qualifier if it equals "SNAPSHOT" or
will remove "-SNAPSHOT" from the end of the qualifier.

The default is no substitution.

For example, the bundle's version can be set with a "SNAPSHOT" qualifier indicating it is
a maven snapshot version.

	Bundle-Version: 3.2.0.SNAPSHOT

When time to release, the `-snapshot` instruction can be used, in a central location such
as `cnf/build.bnd`, to substitute "SNAPSHOT" for another value such as the timestamp of
the build:

	-snapshot: ${tstamp}

or some other meaningful value:

	-snapshot: REL

Using "SNAPSHOT" in the bundle version's qualifier and the `-snapshot` instruction 
are generally for bundles intended for maven builds.
