---
layout: default
class: Macro
title: replace ';' LIST ';' REGEX (';' STRING (';' STRING)? )?
summary: Replace elements in a list when it matches a regular expression
---

    replace ; <list> ; <regex> [ ; <replacement> [ ; <delimiter> ] ]

Replace all parts within elements of the list that match the regular expression regex with the replacement. The `replace` macro uses a simple splitter, the regular expression `\s*,\s*`, to split the list into elements. So any comma in the input list will be use to split the list into elements. See [replacelist](replacelist.html) for an equivalent macro which has a more sophisticated splitter which takes quoted sections of the string into account.

The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `element.replaceAll(regex,replacement)` method to do the replacement. The default replacement is the empty string. The default delimiter is ",".

## Examples

    impls: foo,bar
    ${replace;${impls};$;.jar} => foo.jar,bar.jar

