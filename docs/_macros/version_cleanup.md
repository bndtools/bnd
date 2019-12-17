---
layout: default
class: Macro
title: version_cleanup ';' VERSION
summary: Cleanup a potential maven version to make it match the OSGi Version syntax.
---

The `version_cleanup` macro takes a version-ish string and cleans it up, producing the OSGi Version syntax. 

For example, a Maven version can be turned into the OSGi Version syntax:

	${version_cleanup;1.2.3-SNAPSHOT} -> 1.2.3.SNAPSHOT

1. If the argument passed in is `null`, the version returned is `0`.
2. If the argument passed is a valid OSGi version _range_ the range is returned unaltered.
3. If the argument is a version range matching the regular expression `(\\(|\\[)\\s*([-.\\w]+)\\s*,\\s*([-.\\w]+)\\s*(\\]|\\))` (with `java.util.regex.Pattern.DOTALL` enabled)  a sufficiently cleaned up OSGi Version range is returned.
4. If the argument is a version string matching the regular expression `(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^\\p{Alnum}](.*))?` (with `java.util.regex.Pattern.DOTALL` enabled) a sufficiently cleaned up OSGi Version string is returned.

