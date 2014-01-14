# Getting Started
## Introduction
bndoc is a utility to convert one or more [markdown][1] documents into:

* A single HTML file
* A PDF file
* A multi page HTML

It supports basic markdown with a number of extensions:

* Tables
* Simple Diagrams from ASCII art
* Definition lists
* Section numbering
* Table of contents generation
* Templating
* Macros

## Naming
The is bndoc (should be written with lower case), pronounced as bee-an-doc, similar to bnd
and jpm. 

## Installing
You can install bndoc from [jpm4j][2]. 

    $ sudo jpm install bndoc
    $ bndoc version
    ${block;80;2;4;${system;bndoc version}}

Despite the `$`, this is not restricted to *nix'es, there is also support for Windows on [jpm4j][3].
Alternatively, you can download `bndoc` from the the jpm4j website at:

	http://jpm4j.org/#!/p/osgi/biz.aQute.bndoc.run

After you downloaded the JAR, you can execute it as follows:

    $ java -jar biz.aQute.bndoc.run.jar help

In the coming sections it is assumed you have the `bndoc` command available in your path.

## Command line
When you run `bndoc` from the command line without given it any arguments then it 
will show the help text.

    $ bndoc
    ${block;80;4;4;${system;bndoc}}

### Main Options
There are a number of options that can be specified directly after the command but before the sub
command. These options are valid for all sub-commands.

+========+===============+===================+================================================+
| Letter | Name          | Argument          | Description                                    |
+--------+---------------+-------------------+------------------------------------------------+
| `-b`   | `--base`      | `<string>`        | Specify a base directory, this directory will  |
|        |               |                   | be treated as the 'default' working directory  |
+--------+---------------+-------------------+------------------------------------------------+
| `-e`   |`--exceptions` |                   | Print exception stack traces when they occur.  |
+--------+---------------+-------------------+------------------------------------------------+
| `-f`   |`--failok`     | `<string>`        | Do not return error status for error that match|
|        |               |                   | this given regular expression.                 |
+--------+---------------+-------------------+------------------------------------------------+
| `-k`   |`--key`        |                   | Wait for a key press, might be useful when you |
|        |               |                   | want to see the result before it is overwritten|
|        |               |                   | by a next command.                             |
+--------+---------------+-------------------+------------------------------------------------+
| `-p`   |`--pedantic`   |                   | Be pedantic about all diagnostic details.      |
+--------+---------------+-------------------+------------------------------------------------+
| `-t`   |`--trace`      |                   | Trace on                                       |
+--------+---------------+-------------------+------------------------------------------------+
| `-w`   |`--width`      |                   | Output width (used for printouts, no guarantee)|
+--------+---------------+-------------------+------------------------------------------------+


### `credits`
Some of the open source licenses require that a program gives credits. The `credits` command
tells in detail what open source packages have been used.

    $ bndoc credits
    ${block;80;3;4;${system;bndoc credits}}
 
###`html`
The `html` command is used to create a directory with a single HTML file and its resources. The
processor creates this single HTML file by processing the _sources_. The sources are given
on the command line, yuo can specify one or more sources. 

	bndoc html doc/frond.md doc/en/**.md

A source can be one of the following:

file_path
:	A file path.  

dir_path
:	A directory specification. The directory is expanded. Members are sorted and only one level
	is used. 

dir_path/[*]*[.ext]
:	A directory specification. If 2 wildcards are use (dir/**) all sub directories are recursively
	traversed in sorted order. You can match the file name with a globbing expression. For example
	`bndoc dir/*.md`.

The paths can be relative to the current directory (you can override the current directory 
with `-b` on the command line) or absolute. Supports special characters like `..` and `~`  
in file and dir paths. 
	


	


[1]: http://daringfireball.net/projects/markdown/
[2]: http://jpm4j.org/#!/p/osgi/biz.aQute.bndoc.run
[3]: http://jpm4j.org/#!/md/windows

