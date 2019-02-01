---
layout: default
class: Macro
title: replacelist ';' LIST ';' REGEX (';' STRING (';' STRING)? )?
summary: Replace elements in a list when it matches a regular expression
---

    replacelist ; <list> ; <regex> [ ; <replacement> [ ; <delimiter> ] ]

Replace all parts within elements of the list that match the regular expression regex with the replacement. The `replacelist` macro uses a sophisticated splitter to split the list into elements. This splitter understands quoted sections within the list and does not split on commas inside the quoted sections.

The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `element.replaceAll(regex,replacement)` method to do the replacement. The default replacement is the empty string. The default delimiter is ",".

## Examples

    impls: foo;version="[1,2)", bar;version="[1.2,2)"
    ${replacelist;${list;impls};$;\\;strategy=highest} =>
      foo;version="[1,2)";strategy=highest,bar;version="[1.2,2)";strategy=highest

