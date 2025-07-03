---
layout: default
class: Project
title: -stalecheck srcs ';newer=' depends ( ';' ( warning | error | command ))* ...
summary: Perform a stale check of files and directories before building a jar 
since: 4.3
---

If you work in Bndtools then there is normally little to worry about, bndtools keeps everything up to dated all the time. 
Any file you save, and guaranteed in s short time all dependent bundles are updated. This comes with a crucial guarantee 
that anything you do in Bndtools is faithfully reproduced on the build server using Gradle. For a default workspace there 
is no need for any gradle specific files. All the workspace's settings are defined in the bnd files.

However, sometimes it is necessary to generate sources that need to be compiled. There are then two choices when you 
checkout a workspace. Always use the command line let the build do its magic or checkin the generated sources. 
Although generally frowned upon, it has the advantage that you do not need to store the tool itself under version control. 
Any revision can always compile regardless of what happened to the tool. It is also nice to see it on Github, it actually 
makes the experience of the majority of people that use the workspace and usually not touch the swagger file blithely 
unaware of the underlying complexities.

However, this has a very serious problem. Sometimes you touch the swagger file and forget to run the gradle to generate 
new sources. It would be very convenient if bnd could generate an error if certain files were out of date respective
to a set of other files.  Thhe `-stalecheck` instruction has this purpose.

	-stalecheck	::= 	clause ( ',' clause )*
	clause		::= 	src ';' 'newer=' depends (';' option )*
	src		::=	Ant Glob spec
	depends		::= 	Ant Glob spec
	option		::= 	'error=' STRING 
			| 	'warning=' STRING 
			| 	'command=' STRING


Just before bnd will start making the JAR(s) for a project it will check if any of the files in the `src` specification 
is newer than the files in the `depends` set. If so, it will provide warning or error. If either a specific error or 
warning `STRING` is given then this is reported accordingly.

If a command `STRING` is given it is executed as in the `${system}` macro. If the command `STRING` starts with a 
minus sign (`-`) then a failure is not an error, it is reported as warning.

If no command is given, nor an error or warning then a default error is reported.

## Examples

    -stalecheck:  \   
        openapi.json; \ 
            newer='gen-src/, checksum.md5' 

    -stalecheck:   \
        specs/**.md; \ 
            newer='doc/**.doc' ; \ 
            error='Markdown needs to be generated'


