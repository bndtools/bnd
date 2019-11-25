---
title: Macro Reference
layout: default
---


A simple macro processor is added to the header processing. Variables allow a single definition of a value, and the use of derivations. Each header is a macro that can be expanded. Notice that headers that do not start with an upper case character will not be copied to the manifest, so they can be used as working variables. Variables are expanded by enclosing the name of the variable in `${<name>}` (curly braces) or `$(<name>)` (parenthesis). Additionally, square brackets \[\], angled brackets <>, double guillemets «», and single guillemets ‹› are also allowed for brackets. If brackets are nested, that is `$[replace;acaca;a(.*)a;[$1]]` will return `[cac]`.

There are also a number of macros that perform basic functions. All these functions have the following basic syntax:

     macro ::= '${' function '}' 
         | '$\[' function '\]'
         | '$(' function ')'
         | '$<' function '>'

     function ::= name ( ';' argument ) *

For example:

    version=1.23.87.200109111023542
    Bundle-Version= ${version}
    Bundle-Description= This bundle has version ${version}

## Macro patterns
The default macro pattern is the `${...}` pattern, a dollar sign ('$') followed by a left curly bracket ('{') and closed by a right curly bracket ('}'). However, since bndlib is often used inside other systems it also supports alternative macro patterns:

* `$(...)`, 
* `$<...>`, 
* `$[...]`, 
* `$«..»` (pointing double angle quotation mark \u00AB abd \u00BB), and
* `$‹...›` (single pointing angle quotation mark)


## Arguments
@Since("2.3") Macros can contain arguments. These arguments are available in the expansion as ${0} to ${9}. ${0} is the name of the macro, so the actual arguments start at 1. The name is also available as ${@}. The arguments as an array (without the name of the macro) is available as ${#}. The more traditional * could not be used because it clashes with wildcard keys, it means ALL values. 

For example:

    foo: Hello ${1} -> ${foo;Peter} -> "Hello Peter"
    
## Wildcarded Keys
Keys can be wildcarded. For example, if you want to set -plugin from different places, then you can set the `plugin.xxx` properties in different places and combine them with `-plugins= ${plugins.*}`.


## Expansion of ./

The `./` sequence is automatically expanded to the current filename when found in a macro source file. This generally what you want but unfortunately not always. The `./` prefix is only replaced when:

* It is at the start of the expansion, or
* Preceded by a whitespace.

So, do you really require the `./` without expansion? If so, then there are the following solutions. The first one is to use another macro to break the sequence:

	.=.
	Some-Header: ${.}/conf/admin.xml

Alternatively there are a couple of macros that return the given value when called appropriately, and thereby break the sequence:

	Some-Header-1: ${def;.;.}/conf/jetty/admin.xml
	Some-Header-2: ${uniq;.}/conf/jetty/admin.xml
	Some-Header-3: ${unescape;.}/conf/jetty/admin.xml

## Booleans

In many places a header is used to indicate false or true. In those cases we use some heuristics. The header/macro or whatever is false when:

*	not set
*   empty string
*	'false'
*	'!'
*	'off'
*	'not'

If the value starts with `!` and text follows, the `!` is removed and the remaining text is interpreted as a boolean and then negated.

In other cases, the value is considered `true`.

## Types
@TODO




