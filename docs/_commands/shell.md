---
layout: default
title:  shell [options]
summary: Open a shell on a project, workspace, or plain bnd defaults and exercise commands and macros 
---

## Description

The `shell` function in bnd is primarily intended to exercise macros. Although the `macro` command made it possible to test a single macro, the awful interaction between the (ba)sh character interpretations for $ and quotes made this quite hard to use in practice. the shell therefore directly _talks_ the macro language as you write it in a bnd.bnd file. Additionally, all bnd commands are also available.

    $ bnd shell
    Base Project com.example.project
    > p
    com.example.project
    > now
    Fri Sep 28 11:29:02 CEST 2018
    >

When you start the shell bnd will try to find a project. If the `-p` options is specified it will first look in that directory, otherwise it will look in the current working directory. If no project is found, it will try to find the workspace set by bnd. If no workspace can be found, bnd will use the bnd defaults as properties.

### Properties

A project inherits all properties from the workspace. So when bnd has a project in _scope_ then all macros and properties are available defined in the project's `bnd.bnd` file, the `./cnf/build.bnd` file, and any files in `./cnf/ext/*.bnd`. For example, `javac.source` is a property set by the workspace:

    > javac.source
    1.8
    >

This raises the question: What properties are there? The shell also supports bnd commands and there is a `properties` command. However, there are a large number of properties so lets limit it to the properties that start with java:

    > properties -k java*
    javac.compliance                         1.8
    javac.source                             1.8
    javac.target                             1.8
    >

## Synopsis

## Options

    -p, --project  path-to-project

## Examples