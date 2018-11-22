---
layout: default
title:            enroute ... 
summary:  enRoute commands
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -a, --anyname ]          - In general a workspace should follow the rules
                                for Bundle Symbolic%nNames. If this option is
                                set any name (including one with file
                                seperators) is fine.
    [ -f, --force ]            - Update a workspace overwrite all existing files.
    [ -s, --single ]           - Create a single workspace for the Eclipse
                                workspace and the bnd workspace. This is not
                                recommended because if you fully clean the
                                directory you delete Eclipse metadata.
    [ -u, --update ]           - Existing files are updated when they are older
                                than the ones in the template

## Examples

    biz.aQute.bnd (master)$ bnd enroute help

    OSGi enRoute Commands

    Available sub-commands: 

      workspace                   -  

    biz.aQute.bnd (master)$ bnd enroute help workspace

    NAME
      workspace                   - Create a workspace in the base directory
                                    (working directory or set with bnd -b <base>).
                                    The name of the workspace should be a Bundle
                                    Symbolic Name type (like com.example.whatever).
                                    If another type of name is necessary you can
                                    override it with --anyname. Two directories will
                                    be created. One for the bndworkspace and the
                                    other for the eclipse workspace. Having two
                                    directories makes life a loteasier when you use
                                    git, it allows you to clear the bnd workspace
                                    with 'git clean -fdx' withoutkilling any
                                    personal Eclipse data. It also prevents you from
                                    accidentally storing thispersonal data in git.
                                    If you know better, use --single to let these
                                    workspaces overlap.
    The directory for the workspaces must be empty unless you specify --update or
      --force.%nThis template will also install a gradle build system and a travis
      continuous integrationcontrol file.%nA workspace cannot be created at the root
      of a file system. The general layout of thefile system is%n%n%n ../wss/%n
      com.acme.prime/%n .metadata/%n ....%n scm/%n cnf/%n build.bnd%n ....%n
      com.acme.prime.runner.api%n

    SYNOPSIS
      workspace [options] <workspace>

