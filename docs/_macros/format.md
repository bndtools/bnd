---
layout: default
class: Macro
title: format ';' STRING (';' ANY )* 
summary: Print a formatted string using Locale.ROOT, automatically converting variables to the specified format if possible.
---

The `format` macro can be used to format a string using the `java.util.Formatter`.
The `Locale.ROOT` is used for build reproducibility.
The arguments after the format string are converted from String to an appropriate format useful by the conversion in the format string.
