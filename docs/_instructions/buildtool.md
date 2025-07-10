---
layout: default
title: -buildtool  toolspec  (EXPERIMENTAL!)
class: bnd
summary: |
   A specification for the bnd CLI to install a build tool, like gradle, in the workspace
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-buildtool \
        gradle; version=7.3.0; \
        bnd_version = 6.1.0
        bnd_snapshot=https://bndtools.jfrog.io/bndtools/libs-snapshot-local`

- Pattern: `.*`

<!-- Manual content from: ext/buildtool.md --><br /><br />

In many projects, the workspace carries the files to build it with an external tool. Gradle is the most
popular but it is also possible to use other tools. An advantage of the workspace is that there are
no specific setups required, the workspace contains all the information needed to build. For example,
the bnd command line can build it. Tools are generally used to provide additional tasks.

The setup files of the build tool therefore tend to be dead weight. Dead weight, that frequently needs
overlapping properties with the bnd workspace. Since the tools tend get updated frequently, there are
many software engineering advantages in installing the build tool before the project is being built. It
also makes it easier to maintain many different workspace, they no longer need to be updated when a
new release becomes available. Only the workspace `cnf/build.bnd` needs to be maintained.

## Github Actions

TBD: a github action that will run the bnd command is foreseen

## -buildtool instruction

The `-buildtool` instruction provides the information for the bnd command line tool to install a
build tool template from the net. It has the following attributes:

* `version` – If the `version` is an OSGi version, the key _must_ be a tool name, for example `gradle`. The url used
  to download the template will then be constructed of a url to 
       `https://github.com/bndtools/workspace.tool.${key}/archive/refs/tags/${version}.zip`
  
  If it is not a version, it can be:
  * `url` – The key is interpreted as a URL to a template zip file.
  * `file` – The key is interpreted to a file local to the root of the workspace
* `*` – Any other attributes will be expanded in a properties file

## Template

The template zip must contain a `tool.bnd` file. This file describes how the template should be installed. This file
can be anywhere in the zip hierarchy, its parent directory is the _root_. Only files from the _root_ will be copied.

The `tool.bnd` file must contain a `-tool` instruction. 

    -tool \
        .*;tool.bnd;skip=true, \
        gradle.properties;macro=true,\
        gradlew;exec=true, \
        *

This is a normal _instructions_. The keys are matched against the file names relative from the _root_. The first matching
instruction, provides the instructions how to process the file. The following attributes are available.

* `skip=*` : Skip these files
* `macro=*` : Process macros. The macros can use the workspace macros and any macros specified in the `tool.bnd` file.
* `append=*` : Append the extra attributes of the `-buildtool` instruction to the matching files. This will happen in the format of bnd/properties files.
* `exec=*` : Make the file executable for the owner

The attributes _must_ have a value, this value is ignored.

## Example

We want to use Gradle 7.3 with bnd 6.1.0. We therefore add the following to the bnd workspace `cnf/build` file:

    -buildtool \
        gradle; version=7.3.0; \
        bnd_version = 6.1.0
        bnd_snapshot=https://bndtools.jfrog.io/bndtools/libs-snapshot-local

To install the tool, you can execute the bnd command `buildtool`:

    $ bnd buildtool --force

This will download the sources at `https://github.com/bndtools/workspace.tool.gradle/archive/refs/heads/7.3.0.zip`. The zip file
looks something like:

    workspace.tool.gradle/
        tool.bnd
        gradle/
            wrapper/
                gradle-wrapper.jar
                gradle-wrapper.properties
        gradle.properties
        settings.gradle
        gradlew.bat
        gradlew
        .gitignore

The  `tool.bnd` looks like:

    -tool \
        .*;tool.bnd;skip=true, \
        gradle.properties;macro=true,\
        gradlew;exec=true, \
        *

It should be clear that the `gradle.properties` is expanded. After the files are copied, it looks like:

    # Appended by tool manager at 2021-12-02T14:40:55.660843Z
    version = url
    bnd_version = 6.1.0
    bnd_snapshots = https://bndtools.jfrog.io/bndtools/libs-snapshot-local

After the command has finished, the gradle command can be executed

    $ ./gradlew --parallel clean build
    
