---
layout: default
class: Project
title: -stalecheck ( srcs ( ';' ( newer | warning | error | command )) * ) *
summary: Perform a stale check of files and directories before building a jar 
---

The `-stalecheck` verifies two file sets, the _src_ and _newer_ set. If any of the files in the src set is
newer than any of the files in the newer set it triggers either a warning, an error, or a system command.

   stalecheck ::= src ( ';' attributes  ) +
   
   src        ::= // Ant wildcard for the src files/directories
   attributes ::= newer | error? | warning? | command?
   newer      ::=  // Ant wildcard for the files that depend on src
   error      ::= 'error=' STRING
   warning    ::= 'warning=' STRING
   command    ::= 'command=' ('-')? STRING 

The `error` or `warning` attributes specify a message that is logged when the src set is out of date
with respect to the newer set.

The `command` is executed when the newer set is stale. If the command is prefixed with a minus sign (`-`) then
any errors will be ignored.

## Examples

    -stalecheck:  \   
        openapi.json; \ 
            newer='gen-src/, checksum.md5' 

    -stalecheck:   \
        specs/**.md; \ 
            newer='doc/**.doc' ; \ 
            error='Markdown needs to be generated'


