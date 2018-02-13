---
layout: default
class: Macro
title: subst ';' STRING ';' REGEX (';' STRING (';' NUMBER )? )?
summary: Substitute all the regex matches in the target for the given value; if a count is specified, limit the number of replacements to that count.
---

	subst ; <target> ; <regex> [ ; <replacement> [ ; <count> ] ]
	
Substitute the substrings that match the `<regex>` in the the `<target>` with the replacement. The default replacement is the empty string. The default count is all. 


## Examples

	${subst;foo.bar;.bar}       =>  foo
	