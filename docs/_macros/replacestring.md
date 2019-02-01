---
layout: default
class: Macro
title: replacestring ';' STRING ';' REGEX (';' STRING )?
summary: Replace elements in a string when it matches a regular expression
---

    replacestring ; <string> ; <regex> [ ; <replacement> ]

Replace all parts of the string that match the regular expression regex with the replacement. The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `string.replaceAll(regex,replacement)` method to do the replacement. The default replacement is the empty string.

## Examples

    description: This is, possibly, the best implementation ever!
    ${replacestring;${description};possibly;definitely} =>
       This is, definitely, the best implementation ever!

