---
layout: default
class: Project
title: -fixupmessages SELECTOR ( ';' ( is | replace | restrict ) )* ... 
summary: Fixup errors and warnings. 
---

The `-fixupmessages` instruction is intended to _fixup_ the errors and warnings. It allows you to remove errors and/or warnings, turn errors into warnings, and turn warnings into errors. With this instruction you can fail a build based on a warning or succeed a build that runs into errors.

The default of this instruction is to list a number of patterns. Any error or warning that matches this pattern is then removed. The following example will remove any  error/warning that matches `'some error'`, `'another error'`, or `'and yet another error'`.

	-fixupmessages:  \
		some error, 
		another error, 
		and yet another error

The pattern is a [SELECTOR][1], which makes it possible to do case insensitive matches, wildcards, literals, etc.

## Syntax

The basic format of `-fixupmessages` is:

	-fixupmessages ::= fixup ( ',' fixup ) *
	fixup          ::= SELECTOR directive *
	directive      ::= ';' ( restrict | is | replace )
	restrict       ::= 'restrict:=' ( 'error' | 'warning' )
	is             ::= 'is:=' ( 'ignore' | 'error' | 'warning' )
	replace        ::= 'replace:=' <<text>> 


The value of a fixup clause is a globbing expression.

## Directives

The following directives are supported:

* `restrict:` – By default, the fixup clause is applied to all errors and warnings. You can restrict its application to either errors or warnings specifying either `restrict:=error` or `restrict:=warning`.
* `is:` – By default an error remains an error and a warning remains a warning. However, if you specify the `is:` directive you can force an error to become a warning or vice versa. This can be very useful if you build fails with an error that you do not consider a failure.
* `replace:` – Replace the message with a new message. The replacement will be processed by the macro processor, the `${@}` macro will contain the current message.

The `-fixupmessages` instruction is a _merged property_. This means that you can define it in many different places like for example in a file in `cnf/ext`. Just put an extension on the instruction. For example:

	-fixupmessages.one: 'Some error'
	-fixupmessages.two: 'Another error'

## Examples

	# Turn an error into a warning
	-fixupmessages
	  "Invalid character'; \
	    restrict:=error;
	    is:=warning
  
	# Replace a message
	-fixupmessages \
	  "split";replace:=broken
	  
	# Ignore case by appending :i
	-fixupmessages \
	  "case insensitive:i"
	
	# Wildcards
	-fixupmessages \
	  "prefix*suffix"

	# Turn properties parser messages into warnings
	-fixupmessages.parser: \
	  "Invalid character in properties"; \
	  "No value specified for key"; \
	  "Invalid property key"; \
	  "Invalid unicode string"; \
	  "Found \\<whitespace>";
	    is:=warning
	    
[1]: https://bnd.bndtools.org/chapters/820-instructions.html#selector
