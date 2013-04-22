# jpm

jpm is the main command. It must be installed to be useful, see [install](#!/md/install). The command maintains a repository on a location that depends on the type of the OS. For example, on MacOS the local information is in the `/Library/Java/PackageManager` directory. 

The purpose of the jpm command is to maintain a set of commands and services available via the command line.

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
    
    NAME
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

## Configuration
Several properties can be configured for jpm. The preferred method to configure jpm is via jpm itself:

    $ jpm settings <key>=<value>

* `jpm.runconfig=<local|global>` - Switch between local (user) and global run mode (also possible with `jpm setup <local|global>`).
* `jpm.cache.<local|global>=...` - Specify cache directory for local/global run mode.
* `jpm.bin.<local|global>=...` - Specify bin directory for local/global run mode.

Current settings can be displayed with the `jpm settings` command without argument. 

