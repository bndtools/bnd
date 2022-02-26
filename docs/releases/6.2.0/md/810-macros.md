___
___
# Macros

A simple macro processor is added to the header processing. Variables allow a single definition of a value, and the use of derivations. Each header is a macro that can be expanded. Notice that headers that do not start with an upper case character will not be copied to the manifest, so they can be used as working variables. Variables are expanded by enclosing the name of the variable in `${<name>}` (curly braces) or `$(<name>)` (parenthesis). Additionally, square brackets \[\], angled brackets <>, double guillemets «», and single guillemets ‹› are also allowed for brackets. If brackets are nested, that is $[replace;acaca;a(.*)a;[$1]] will return `[cac]`.

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

## Arguments
@Since("2.3") Macros can contain arguments. These arguments are available in the expansion as ${0} to ${9}. ${0} is the name of the macro, so the actual arguments start at 1. The name is also available as ${@}. The arguments as an array (without the name of the macro) is available as ${#}. The more traditional * could not be used because it clashes with wildcard keys, it means ALL values. 

For example:

    foo: Hello ${1} -> ${foo;Peter} -> "Hello Peter"
    
## Wildcarded Keys
Keys can be wildcarded. For example, if you want to set -plugin from different places, then you can set the `plugin.xxx` properties in different places and combine them with `-plugins= ${plugins.*}`.


## Types
@TODO

## Base Macros
The following macros are always available.


## Build Macros

### `bsn    																			      {#bsn}
Return the current Bundle Symbolic Name assumed by the code. This tries to load the current `Bundle-SymblicName` property, or reverts an inferred name from the file system.

    local = ${bsn}

## Project Macros

    ${basename;arg
<td>`arg ( ';' arg )*`</td>
<td>Return a comma separated list with all the file names (not directory) of the arguments. Non existent files are skipped.</td>

[[#cat]]`cat`
<td>`arg`</td>
<td>If arg is a directory, return the contents of the directory. If it is a file, return the contents of the file.</td>

[[#classes]]`classes`
<td>QUERY</td>
<td>Provides a query function to find classes to fulfill certain criteria. See the [classes][#classesx] macro</td>

[[#def]]`def`
<td>`arg`</td>
<td>Provide the empty string if arg is not defined, otherwise it provides the value of the property `arg`.</td>

[[#dir]]`dir`
<td>`arg ( ';' arg ) *</td>
<td>Get the directory names of the arguments in a comma separated format. For example, ${dir;${project}} should provide the directory path (absolute) of the directory that contains the project</td>

[[#env]]`env`
<td>; name</td>
<td>Provide the value of the given environment variable</td>

[[#error]]`error`
<td>`arg ( ';' arg ) *`</td>
<td>Generate an error for each `arg`.</td>

[[#if]]`if`
<td>; condition ; true ( ; false ) ? </td>
<td>If the condition is not empty, the true part is returned, else the false part is returned. If not false part is supplied, the empty string is returned. The condition is trimmed before tested. For example:</td>

  Comment: ${if;${version};Ok;Version is NOT set###!}

[[#isdir]]`isdir`
<td>`arg`</td>
<td>Returns "true" when arg is an existing directory, otherwise "false".</td>

[[#isfile]]`isfile`
<td>`arg`</td>
<td>Returns "true" when arg is an existing file, otherwise "false".</td>

[[#filter]]`filter`
<td>';' list ';' regex</td>
<td>The filter macro iterates over the given list and only includes elements that match the given regular expression (regex). The following example includes only the jar files from the list: \\</td>
`list= a,b,c,d,x.jar,z.jar \\
List= ${filter;${list};.*\\.jar}`

[[#filterout]]`filterout`
<td>; list ; regex</td>
<td>The filterout macro iterates over the given list and removes elements that match the given regular expression (regex). The following example strips the jar files from the list:  \\</td>
`list= a,b,c,d,x.jar,z.jar \\
List= ${filterout:${list};.*\\.jar}`

[[#findname]]`findname`
<td>; regex [ ; replacement ]  </td>
<td>Find the paths to any resources that matches the regular expression, replace the name with the replacement of the regex. Notice that the regex is only executed on the name of the resource, that is, without the slashes.</td>

[[#findpath]]`findpath`
<td>; regex [ ; replacement ]  </td>
<td>Find the paths to any resources that matches the regular expression, replace the path with the replacement of the regex. Notice that the regex is executed on the path of the resource, that is, with the slashes.</td>

[[#fmodified]]`fmodified`
<td>; file-path-list </td>
<td>Return the highest modification time of the given file path. The returned value is based on the epoch of Java, it is therefore a long.  \\</td>
`Last-Modified: ${long2date;${fmodified;${files}})`

[[#githead]]`githead`
<td></td>
<td>Returns the SHA of the head. This is either HEAD or the contents of the sym-ref. \\</td>
`Git-HEAD: ${githead}`
<td>2.3</td>

[[#join]]`join`
<td>( ; list ) *</td>
<td>Joins a number of lists into one. It may seem that this can be easily accomplished by just placing two macro expansions after each other. The result of this will not be a list, unless a ',' (colon) is placed in between. However, when one of the lists is empty, the colon will be superfluous. The join handles these cases correctly. Any number of lists may be given as arguments. \\</td>
`List= ${join;a,b,c;d,e,f}`

[[#literal]]`literal`
<td>`arg`</td>
<td>Provide a literal macro for arg. For example, `${literal;"project"}` results in `"${project}"`. Can be useful if information must be created for other systems that uses the macro syntax.</td>

[[#lsa]]`lsa`
<td>`dir` ( ';' PATTERN ( ',' PATTERN )* )?</td>
<td>Return the contents of the given directory filtered by the patterns. The result is a comma separated list of absolute file names.</td>

[[#lsr]]`lsr`
<td>`dir` ( ';' PATTERN ( ',' PATTERN )* )?</td>
<td>Return the contents of the given directory filtered by the patterns. The result is a comma separated list of relative file names.</td>

[[#long2date]]`long2date`
<td>; long </td>
<td>Parse the long and turn it into a date.  \\</td>
`Last-Modified: ${long2date:${fmodified:${files}})`

[[#maven_version]]`maven_version`
<td>version</td>
<td>Clean up a maven version so it conforms to the OSGi specification.</td>
<td></td>


[[#now]]`now`
<td></td>
<td>Returns the current Date as string.  \\</td>
`Created-When: ${now}`

[[#osfile]]`osfile`
<td>`base (';' PATH )*`</td>
<td>Return a comma separated list of absolute file paths in the current's OS' format. If the PATH is relative, it is calculated from the base directory.</td>

[[#path]]`path`
<td>LIST (';' LIST )*`</td>
<td>Return a platform specific path (File.pathSeparator) from the given lists.</td>


[[#pathseparator]]`pathseparator`
<td></td>
<td>Returns the current path separator as defined by `File.pathSeparator`</td>

[[#permissions]]`permissions`
<td>`(all|packages|admin)+`</td>
<td>Generates a permission file. If packages is specified, it provides PackagePermission for imports and exports. `admin` adds an AdminPermission, `all` adds them all</td>
<td></td>


[[#range]]`range`
<td>; ('[' | '(') MASK ',' MASK (']' | ')') [ ; VERSION]</td>
<td>Create a version range from a version. The MASK is defined in [${version}][#version].</td>
   
[[#replace]]`replace`
<td>; list ; regex ; replacement </td>
<td>Replace all elements of the list that match the regular expression regex with the replacement. The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `item.replaceAll(regex,replacement)` method to do the replacement.For example, to add a `.jar` extension to all files listed, use the following: \\</td>
`List = ${replace;${impls};$;.jar}`

[[#sort]]`sort`
<td>; list</td>
<td>Sort the given list using string sorting collation. For example: \\</td>
`List= ${sort:acme.jar, harry.jar, runner.jar, alpha.jar, bugs.jar}`

[[#system]]`system`
<td>system cmd</td>
<td>Execute a command via `System.exec` and return the result as the macro's value.</td>

[[#toclassname]]`toclassname`
<td>; list</td>
<td>Replace a class path (with slashes and class at the end) to a class name (with dots).</td>

[[#toclasspath]]`toclasspath`
<td>; list  [ ; suffix ]</td>
<td>Replace a class name (with dots) to a classpath (with slashes and `suffix` at the end). The default suffix is `.class`. The suffix may be empty</td>

[[#tstamp]]`tstamp`
<td> ( date pattern )?</td>
<td>Answer a time stamp based on the pattern. The pattern is defined by the [SimpleDateFormat][http://download.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html]. The default pattern is `yyyyMMddHHmm`.</td>


[[#unique]]`unique`
<td>; list ( ; list ) *</td>
<td>Split all the given lists on their commas, combine them in one list and remove any duplicates. The ordering is not preserved, see [${sort}][#sort] For example:</td>
  
  ${unique; 1,2,3,1,2; 1,2,4 } ~ "2,4,3,1"

[[#version]]`version`
<td>; mask ; version</td>
<td>This macro can modify a version by dropping parts from the end, incrementing parts, or decrementing parts. The mask is a string containing from 1 to 4 characters. The characters have the following meaning: \\</td>
  = the actual version part \\
  + increment the actual version part \\
  - decrement the actual version part \\
For example, ${version;=+;1.2.3.q} will become 1.3.


[[#warning]]`warning`
<td>`arg ( ';' arg ) *`</td>
<td>Generate a warning for each `arg`.</td>


(:tableend:)

