# Just Another Package Manager for Java
The purpose of the this command is to maintain a set of commands and services available via the command 
line. It has the same purpose as [npm][1] but then for Java. The [jpm4j.org][2] repository contains all
of maven and many other JARs that can now be installed from the command line with a simple command.

You should first install jpm, this is described for the different supported platforms on the [jpm website][3].


After you've installed it, you should be able to do:

    $ jpm version
    1.0.0.201311261409
    $

TBD ...

## Help
To find your way around jpm, you can use help.

    $ jpm help
    Just Another Package Manager (for Java)
    Maintains a local repository of Java jars (apps or libs). Can automatically link
    these jars to an OS command or OS service. For more information see
    https://www.jpm4j.org/#!/md/jpm

    Available commands: 

      candidates                  - List the candidates for a coordinate 
      ...

For each command, more extensive help is available with `help <command>`.

    $ jpm help candidates
    NAME
      candidates                  - Print out the candidates from a coordinate
                                    specification. A coordinate is:
      ....

## Installing
The first thing you are likely want to do is installing a command. bnd, the Swiss army knife for OSGi
is a good example to start. We can install bnd simply by:

    $ jpm install  


## Help
jpm has extensive help information. If you just enter the word `help` then a list of commands is given. 

    $ jpm help
    Maintains a local repository of Java jars (apps or libs).
    Can automatically link these jars to an OS command or OS
    service.

    Available commands: artifact, candidates, certificate,
    command, deinit, find, gc, init, install, jpm, keys, log,
    platform, put, register, restart, service, settings, setup, start,
    status, stop, trace, version, winreg

Later version can have additional commands and/or a different text. More detailed information can be obtained with:

    $ jpm help jpm
    NAME
      jpm - Options valid for all commands. Must be given before
      sub command

    SYNOPSIS
       jpm [options]  ...

    OPTIONS
       [ -b, --base <string>] - Specify a new base directory
       (default working directory).
       ...
You can also suffix the `help` command with one of the listed commands:

    $ jpm4j.filemap (master)$ jpm help version
    
    NA
      version - Show the current version. The qualifier
      represents the build date.
    
    SYNOPSIS
        version 

The help information is shown in three sections:

* NAME - Displays the name and descriptive information.
* OPTIONS - If any. Will show in detail what options can be used and what they mean. It can also contain an indication of what parameters are expected.
* SYNOPSIS - A short command line with all the options.

## Types
There are a number of recurring types used in the command line.

* url - A well known url, like 'http://www.jpm4j.org'
* file - A path on the file system in the local standard. That is on Windows use the back slash, on other operating systems use the forward slash.
* coordinates - Coordinates describe a _revision_ or a _program_. A revision is an actual JAR, the program is the project that creates the revisions. A program coordinate consists of a group id and an artifact id, after maven. 

  Some groups are special:
  * `OSGI` : The OSGI group makes the artifact Id a bundle symbolic name. A maven user should never use this group. Any OSGi bundle in jpm4j is automatically available via this group id. 
  * `SHA` : The SHA group turns the artifact Id into a SHA. By definition, a SHA artifact has a version of 0.0.0. A SHA group Id can (obviously) only be used for revisions.

  A program is identified by a group and artifact id. Revisions can also be identified by a _classifier_ and a _version_. A classifier is also a maven concept; versions can occur in maven or in OSGi. A coordinate is a single string without whitespace identifying a program or revision. It must have the following syntax:
  In the case of `maven`, the order is `groupId`, `artifactId`, `classifier`, and then version.

<pre>coordinates ::= NAME | maven
maven         ::= NAME ':' NAME ( ':' NAME ) ( '@' VERSION )</pre>

## Commands

### version
Display the version of the jpm command. This is the current version number and the qualifier is the date.

###




[1]: https://npmjs.org/
[2]: https://www.jpm4j.org
[3]: http://jpm4j.org/#!/md/install