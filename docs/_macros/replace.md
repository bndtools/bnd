---
layout: default
class: Macro
title: replace ';' LIST ';' REGEX (';' STRING (';' STRING)? )?
summary: Replace elements in a list when it matches a regular expression
---

    replace ; <list> ; <regex> [ ; <replacement> [ ; <delimiter> ] ]

Replace all elements of the list that match the regular expression regex with the replacement. The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `item.replaceAll(regex,replacement)` method to do the replacement. The default replacement is the empty string. The default delimiter is ", ".

## Examples

    impls: foo,bar
    ${replace;${impls};$;.jar}       =>  foo.jar, bar.jar
