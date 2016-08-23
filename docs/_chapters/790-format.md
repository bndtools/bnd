---
layout: default
class: Macro
title: File Format 
summary: 
---

# Instructions

The bnd format is very similar to the manifest. Though it is read with the Properties class, you can actually use the ':' as separator to make it look more like a manifest file. The only thing you should be aware of is that the line continuation method of the Manifest (a space as the first character on the line) is not supported. Line continuations are indicated with the backslash ('\' \u005C) as the last character of the line. Lines may have any length. 

The most common mistake is missing the escape. The following does not what people expect it to do:

    Header: abc=def,
      gih=jkl

This is actually defining 2 headers. You can fold lines by escaping the newline:

    Header: abc=def, \\
      gih=jkl

You can add comments with a # on the first character of the line:

    # This is a comment

White spaces around the key and value are trimmed.

See [  Properties][http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html ] for more information about the format.


## Types of Instructions
There are different instructions in the properties file:

||
||!Type ||!Example ||!Description ||
||Manifest headers ||Bundle-Description: ... ||When the first character is a upper case character. These headers are copied to the manifest or augmented by bnd. ||
||Variables ||version=3.0 ||Variables are lower case headers. Headers can contain references to other headers using macro expansion. Variables are not copied to the manifest. See [Macros][#macros] ||
||Directives ||-include: deflts.bnd ||Directives start with a '-' sign. A directive is an instruction to bnd to do something special. See [Directives][#directives] ||

## Parameters

## Basic Types
||[[#list]][[#LIST]]LIST||A comma separated list. Items should be quoted with '"' if the contain commas. In general, a list item can also define attributes and directives on an item.||
||[[#pattern]][[#PATTERN]]PATTERN||A pattern matches some entity: a package, a directory, etc. Patterns are based on Java regular expressions but are preprocessed before compiled. Any dots ('.') are replaced with \. to make them match the input and not act as the 'any character' operator. Any '?' or '*' is prefixed with a dot to make it match any character. As an extra convenience, if the string ends with \..*, an additional pattern is added to match the complete string with out the \..*. The effect is that something like com.acme.* matches com.acme and all its sub packages. It is also to negate a pattern by prefixing it with an exclamation mark ('!'). For example:\\
   `Import-Package: !com.sun.*, *` \\
indicates that any imports to com.sun should not be imported.||
||[[#regex]]REGEX||A regular expressions||

## Selectors
