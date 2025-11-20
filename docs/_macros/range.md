---
layout: default
class: Macro
title: range ';' RANGE_MASK ( ';' VERSION )
summary: Create a semantic version range out of a version using a mask to control the bump of the ceiling
---

The `range` macro takes a version range mask/template and uses it calculate a range from a given version. The primary reason for the `${range}` macro is to be used in the [version policy][1]. With the version policy we have a version of an exported package and we need to calculate the range for that. The rules come from the [consumer][2] or [provider][3] policy. However, this policy can be overridden on the Import-Package header by specifying the version as a range macro:

	Import-Package: com.example.myownpolicy; version="${range;[==,=+)}", *


Since the version for the exported package is set as `${@}`, the macro will calculate the proper semantic range for a provider.

The syntax for the `range` macro is:

	range ::= ( '\[' |'\( ) mask ',' mask ( '\)' | '\]' )
	mask  ::= m ( m ( m )? )? q
	m     ::= [0-9=+-~]
	q     ::= [0-9=~Ss]

The meaning of the characters is:

* `=` – Keep the version part
* `-` – Decrement the version part
* `+` – Increment the version part
* `[0-9]` – Replace the version part
* `~` – Ignore the version part
* `[Ss]` – If the qualifier equals `SNAPSHOT`, then it will return a maven like snapshot version. Maven snapshot versions do not use the `.` as the separator but a `-` sign. The upper case `S` checks case sensitive, the lower case `s` is case insensitive. This template character will be treated as the last character in the template and return the version immediately. 

## Examples

With `${range}`:

- `${range;[==,+);1.2.3}` will return `[1.2,2)`.
- `${range;[===,+++);1.2.3}` will return `[1.2.3,2.3.4)`.
- `${range;[===,+==);1.2.3}` will return `[1.2.3,2.2.3)`.
- `${range;[===,==+);1.2.3}` will return `[1.2.3,1.2.4)`.
- `${range;[=+=,+=+);1.2.3}` will return `[1.3.3,2.2.4)`.

With `${versionmask}`:

- `${versionmask;===S;1.2.3.SNAPSHOT}` will return `1.2.3-SNAPSHOT`.
- `[${versionmask;==;1.2.3},${versionmask;+;1.2.3})` will return `[1.2,2)`.
- `[${versionmask;===;1.2.3},${versionmask;+++;1.2.3})` will return `[1.2.3,2.3.4)`.
- `[${versionmask;===;1.2.3},${versionmask;+==;1.2.3})` will return `[1.2.3,2.2.3)`.
- `[${versionmask;===;1.2.3},${versionmask;==+;1.2.3})` will return `[1.2.3,1.2.4)`.
- `[${versionmask;=+=;1.2.3},${versionmask;+=+;1.2.3})` will return `[1.3.3,2.2.4)`.



Also see [versionmask][5] / [version][4].

[1]: /chapters/170-versioning.html
[2]: /instructions/consumer_policy.html
[3]: /instructions/provider_policy.html
[4]: /macros/version.html
[5]: /macros/versionmask.html



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
