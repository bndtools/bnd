---
title: Instruction
layout: default
---
A bnd instruction is a property that starts with a minus sign ('-'). An instruction instructs bndlib to do something, in general providing parameters to the code. All instructions in bndlib are listed later in this page.

## Syntax

Almost all bndlib instructions follow the general OSGi syntax. However, the bndlib syntax is in general a bit more relaxed, for example you can use either single quotes (''') or double quotes ('"') while OSGi only allows double quotes. Dangling comma's and some other not exactly correct headers are accepted by bndlib without complaining. Values without quotes are accepted as long as they cannot confuse the general syntax. For example, a value like 1.20 does not have to be quoted since the period ('.') cannot confuse the parser on that place, however, a value like [1.3,4) must be quoted since it contains a comma.

In this mannual we adhere to the same conventions as the OSGi specification. This is mostly reflected in the names for the terminals in the syntax specifications. As a reminder we repeat the syntax rules and common terminals:

* `*` – Repetition of the previous element zero or more times, e.g. `( ',' element ) *`
* `+` – Repetition one or more times
* `?` – Previous element is optional
* `( ... )` – Grouping
* '...' – Literal
* `|` – Or
* `[...]` – Set (one of)
* `..` – Range
* `<...>` – Externally defined token, followed with a description
* `~` – Not, negation

The following terminals are pre-defined:

	ws               ::= <see Character.isWhitespace> 
	digit            ::= [0..9]
	alpha            ::= [a..zA..Z]
	alphanum         ::= alpha | digit
	token            ::= ( alphanum | '_' | '-' )+
	number           ::= digit+
	jletter          ::= <see [JSL][1] for JavaLetter>
    jletterordigit   ::= <See [JLS][1] for JavaLetterOrDigit >
    qname            ::= <See [JLS][1] for fully qualified class name >
	identifier       ::= jletter jletterordigit *
    extended         ::= ( alphanum | '_' | '-' | '.' )+
	quoted-string-d  ::= '"' ( ~["\#x0D#x0A#x00] | '\"'|'\')* '"'
	quoted-string-s  ::= ''' ( ~['\#x0D#x0A#x00] | '\''|'\')* '''
	quoted-string    ::= quoted-string-s | quoted-string-d	
	special-chars    ::= ~["\#x0D#x0A#x00:=;,<See [JLS][1] for whitespace>]
	path             ::= special-chars+ | quoted-string
	value            ::= quoted-string | path
	argument         ::= extended  | quoted-string
	parameter        ::= directive | attribute
	directive        ::= extended ':=' argument
	attribute        ::= extended '=' argument
	parameters       ::= parameter ( ',' parameter ) *
	unique-name      ::= identifier ( '.' identifier )*
	symbolic-name    ::= token('.'token)* 
	package-name     ::= unique-name
	
	version          ::= major( '.' minor ( '.' micro ( '.' qualifier )? )? )?
	major            ::= number 
	minor            ::= number
	micro            ::= number
	qualifier        ::= ( alphanum | '_' | '-' )+
	
	version-range    ::= interval | atleast
	interval         ::= ( '[' | '(' ) floor ',' ceiling ( ']' | ')' )
	atleast          ::= version
	floor            ::= version
	ceiling          ::= version
	
White spaces between terminals are ignored unless specifically noted. The only exception is the directive, a directive must be connected to the colon (':'). That is, bndlib stores attributes and directives in the same map and distinguishes them by the fact that directives end in a colon and attributes do not. For example, using a directive like `foo; path := http://www.xyz.com` will not work correctly because the `path :` is not a valid attribute nor directive name. Any value that contains a space, a tab, a comma, colon, semicolon, equal sign or any other character that is part of a terminal in the grammar must be quoted. 

Almost all bndlib instructions follow the general OSGi syntax for a header. 

	-instruction     ::= parameters
	parameters       ::= clause ( ',' clause ) *
	clause           ::= path ( ';' path ) *
	                          ( ';' parameter ) * 

There are a number of short-cuts that are regularly used in bnd commands:

	list             ::= value ( ',' value ) *
	url              ::= <URL>
	file-path        ::= <file path, using / as separator>
	
## Merged Instructions

Most instructions that accept multiple clauses are _merged instructions_. A merged instruction is an instruction which is specified by its prefix but which will merge any property that starts with this prefix followed by a period ('.'). For example, the _base instruction_ `-plugin` instruction accepts `-plugin.git` as well. The reason for this merging is that allows to append the base instruction with instructions from other files. For example, the bnd file in the `cnf/ext` directory are automatically included in the properties of the workspace. Since there can be many files, there would be a need to coordinate the using of a singleton base instruction. As always, singletons suck and in this case we solved it with merged instructions.

Some instructions put, sometimes very subtle, semantics on their ordering. To prevent different results from run to run, we make order the merge properties by placing the base instruction in front and then followed by the values from the other keys in lexically sorted order. For example, `-a`, `-a.x`, and `-a.Z`, and `-a.1` will concatenate the clauses in the order `-a,-a.1,-a.Z,-a.x`. 

Each of the constituents of the merged properties can be empty, this is properly handled by bndlib and will not create a an empty clause as would easily happen when the same thing was done with macros. Empty clauses can help when there is an optionality that depends on some condition. 

As an example, lets set the `-buildpath` instruction:

	-buildpath: com.example.foo;version=1.2
	
	-buildpath.extra: ${if;${debug};com.example.foo.debug\;version=1.2}

This will result in a buildpath of (when debug is not false) of: `com.example.foo;version=1.2, com.example.foo.debug;version=1.2`.

## SELECTOR

If a value in an instrucion has a _scope_ then you will be able to use a _selector_. A scope is a well defined, finite, set. For example, if you use the '-exportcontents' instruction you can specify the packages that can be exported. Since you can only export the packages that are inside your bundle, the scope is the set of package names that were collected so far.

The syntax for a selector is:

	selector ::= '!' ? 
	             '=' ?  
	                  '*' | ( '.' | '?' | '|' | '*' | '$' | . ) * 
	             ( '.*' ) ? 
	             ( ':i' ) ? 

A selector is an expression that is matched against all members of the scope. For example, `com.example.*` will match any package name that starts with `com.example`, for example `com.example.foo`, as well as `com.example` itself. The syntax used to describe the wildcarding is based on a globbing kind model, however, the selector is translated into a regular expression according to some simple rules.

The replacement rules to build the regular expression are as follows:

* `*` – Is replaced with `.*`, matching any number of characters, triggers wildcard match.
* `?` - Is replaced with `.?`, matching any character, triggers wildcard match.
* `|` – Is inserted as is, triggers wildcard match.
* `$` – Escaped to `\$`.
* `.` – The dot is treated as a package segment separator. It is escaped with `\.`. However, if the match ends with `.*`, the replacement will be `(\..*)?`.  This triggers a wildcard match.

If you want to go ballistic with regular expressions, then you can go ahead. As long as the wildcards are triggered by one of the defined characters, the replaced string will be used as a regular expression. 

If the selector ends with `:i` then a case insensitive match is requested, this will ignore any case differences.

A selector can be prefixed with an exclamation mark, this indicates that a match must be treated as a removal operation. To understand removal, it is necessary to realize that the selectors are not declarative, the order of the selectors is relevant.

When bndlib has a scope, it will iterator over each element in the scope and then for element it will iterator over each selector. If there is a match on the scope member then it is always removed from the scope, it will not be matched against later selectors. If the selector starts with an exclamation mark then the member is removed. but it is not treated as a result. Ergo, the exclamation mark allows you to remove members before they are actually matched.

For example, you want to match all implementation packages (i.e. they have `impl` in their name) except when they start with `com.example`. This can be achieved with:

	!com.example.*impl*, *.impl.*
	
Last, and also least, you can prefix the selector with an equal sign ('=') (after the exclamation mark if one is present). This will signal a literal match with the string, no globbing will be done.

The way the period ('.') is treated is kind of special. The reason is that people expect that the selector `com.example.*` actually includes the package `com.example` itself. In bndlib this has always been the case although the OSGi Alliance later decided to not follow this pattern when there was a need for the globbing of packages.

In this example, the first selector will remove any match and ignore it, the second selector now only sees scope members that are fitting in the earlier pattern. 

Selectors are used in virtually any place where there is a reasonable scope. It is a very powerful feature. However, it is also easy to go overboard. If you find you need to use this feature excessively then you are likely overdoing it.

## FILE

One of the most painful thing in making bndlib run anywhere is handling of files. Even though Java has a decent abstraction of the file system, problems with spaces in file names and backslashes in the wrong place have caused an uneven amount of bugs. Ok, this partially because most bndlib developers tend to use Macs, still no excuse and quite embarrassing.

Since bnd files need to be portable across environments we've chosen to use the Unix file separator, the slash (or solidus '/') for more reasons than I can reasonably sum up here (what was Bill smoking when he picked the reverse solidus for file separator!). In bndlib, all file paths (ok, should) always go through a single method that actually parses it and turns it into a Java File object. This allows us to support a number of features in a portable way. The syntax of a file path is therefore:

	file      ::=  ( '~/' | '/' )?  (  ~['/']+ '/' ) * ~['/'] *   

If a file path starts with:

*  A slash ('/') then we go to the root of the current directory's file system. Sorry, there is no way for a windows user to specify another file system since this would break portability.
* '~/', then the remainder of the file path is relative to the user's home directory as defined by Java. To keep your workspace portable, make sure you do not store any information there that might break the build for others. However, this can be useful to store passwords etc.
* Anything else, then the path is relative. It depends in the context what the used root is of the path. In general, if there is a local file that creates this path, then the parent directory of this file is the root. Otherwise, if this path is used in relation a workspace, project, build, or bndrun then the _base_ of each of these entities is used as the root. In other cases it is the actual working directory of the process. 
 
You can use the '..' operator for a segment in the file path to indicate a parent directory. It is highly advised to always use relative addressing to keep your workspace portable. However, there are a number of macros that can be used as anchors:

* `${p}`, `${project} – The directory of the current project
* `${workspace}` – The directory of the current workspace
* `${build} – The directory of the `cnf` directory.

## FILESPEC

A `FILESPEC` defines a set of files relative to a starting point. It can be identical to a FILE for a single file but it has some special syntax to recurse directories. For example, the FILESPEC `foo/bar/**/spec/*.ts` finds all files that end in `spec/*.ts` somewhere below the `foo/bar` directory.

The syntax is as follows:

	FILESPEC  ::= filespec ( ',' filespec )*
	filespec  ::= ( segment '/' ) filematch
	segment   ::= '**' | GLOB
	filematch ::= '**' | '**' GLOB | GLOB

If a segment is `**` then it will match any directory to any depth. Otherwise it must be a GLOB expression, which can also be a literal. The last segment is the `filematch`. This is a GLOB expression on a file name. As a convenience, if it is `**`, any file will match in any directory and if it starts with something like `**.ts` then it will also recurse and match on `*.ts`. That is, the following rules apply to the `filematch`:

	prefix/**              prefix/**/*              
	prefix/**.ts           prefix/**/*.ts              	 

## PATH

A PATH is a _repository specification_. It is a specification for a number of JARs and/or bundles from a repository (well, most of the time). A PATH has the following syntax:

	PATH      ::= target ( ',' target ) *
	target    ::= ( entry | FILE ';' 'version' '=' 'file' )
                  ( ';' PARAMETERS ) *
	entry     ::= symbolic-name ( ';' version ) ? 
	version	  ::= 'version' '=' 
                  ( RANGE | 'latest' | 'project')

A PATH defines a number of bundles with its _entries_. Each entry is either a FILE or a symbolic-name. If only a symbolic name is specified, the default will be the import range from 0.0.0. This selects the whole repository for the given symbolic name. Outside the default, the repository entry can be specified in the following ways:

* RANGE – A version range limits the entries selected from the active repositories.
* `latest` – Use the project's output or, if the project does not exists, get the bundle with the highest version from the repositories.
* `project` – Mandate the use of the project's output


## GLOB

A _globbing_ expression is a simplified form of a regular expression. It was made popular in the Unix shells for file name matching. 

The syntax is as follows:

	GLOB      ::=  ( wildcard | question | subexpr | character )*
	wildcard  ::= '*'
	question  ::= '?'
	subexpr   ::= '{' GLOB ( ',' GLOB )* '}'
	character ::= <unicode>

A `wildcard` matches any number of characters in the input. A `question` matches a single character. A `subexpr` matches the comma separated set of alternatives. Anything else is matched directly,

To match a literal character that is in `[*?{},]`, prefix it with a backslash ('\');

Some examples:

    abc.ts                   abc.ts
	*.ts                     abc.ts, def.ts
	foo*****bar              foobar, fooXbar, fooXXXXbar
	foo{A,B,C}bar            fooAbar, fooBbar,fooCbar
    foo\{\}bar               foo{}bar
