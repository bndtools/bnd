---
layout: default
class: Project
title: -prepare makespec ( ',' makespec )*
summary: Execute a number of shell commands before every build (might not work on Windows)
---

An typical use case for the `-prepare` instruction is the generation of CSS files from a `less` or `sccs` specification. The prepare instruction will execute a number of commands in the shell before it starts the build process.

The commands used in the shell must work on the platform. Since the shell is so different in a Windows environment this function is not guaranteed to work.

## Example

The following example compiles typescript code in the `typescript` directory. The output goes to `web/repository.js`. To install the `tsc` command, see `npm`.

	-prepare: \
		web/foo.js <= typescript/*.ts ; \
			command:=tsc -p typescript --out $@

## Syntax

	-prepare     ::= makespec ( ',' makespec )
	makespec     ::= dependency ( ';' parameter )+
	dependency   ::= PATH ( '<=' FILESPEC )?
	parameter    ::= command | report | name | env
	command      ::= 'command:=' <shell> # mandatory
	report       ::= 'report:=' PATTERN
	name         ::= 'name:=' STRING
	env          ::= key '=' value

A `makespec` defines a dependency in the file system. It must start with a file path, this is the output file of the command.  It can be followed with a `dependency`. A `dependency` is a `FILESPEC`. The `FILESPEC` defines the set of input files to the command. If specified, then the command is only executed when the target file is older than any of the dependencies and the project bnd files.

It is possible to specify a `name` for the command, this name is then used for any error reporting.

The `command` parameter specifies the shell command. It must be a valid shell command. Any `$@` and `$<` macros are replaced by the target file path and the set of files in the dependency.

The shell is supposed to support multiple commands (separate with ';') and pipes ('|'). The working directory the command will run in is the project directory. 

Any attributes (i.e. not directives) are added as an environment variable for the command.

If the executed command provides diagnostic output with file names, line numbers, etc. then it is possible in some cases to report these to the IDE so the error/warning appears on the right place. This requires the specification of a regular expression. This regular expression must specify named groups. A named group is like `(?<name>.*)`. The expression must find error messages from the console output or the error output. 

The following names can be used in the regular expression:

	file           relative file name
	line           zero based line number
	line-1         one based line number
	type           `error` | `warning`
	message        the message

For example, the report pattern for the Microsoft TypeScript compiler tsc:

	"(^|\n)(?<file>[^(]+)\\((?<line>[0-9]+),(?<column>[0-9]+)\\):(?<message>[^\n]*)"

## Path

It is possible to set the platforms command search path with the `-PATH` macro. This macro must contain a comma separated list of PATH, specifying places where to look. If this `-PATH` macro contains `${@}` then this will be replaced with the platforms current PATH. By not specifying the `${@}` it is possible to limit the available commands.
