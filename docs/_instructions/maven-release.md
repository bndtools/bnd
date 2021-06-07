---
layout: default
class: Project
title: -maven-release ('local'|'remote') ( ',' option )*
summary:  Set the Maven release options for the Maven Bnd Repository
---

The `-maven-release` instruction provides the context for a release to Maven repository. In the Maven world it is customary that a release has a JAR with sources and a JAR with Javadoc. In the OSGi world this is unnecessary because the sources can be packaged inside the bundle. (Since the source is placed at a standard location, the IDEs can take advantage of this.) However, putting an artifact on Maven Central requires that these extra JARs are included. This instruction allows you to specify additional parameters for this release process.

Though this instruction is not specific for a plugin, it was developed in conjunction with the [Maven Bnd Repository Plugin][1].


    -maven-release ::= ( 'local'|'remote' ( ';' snapshot )? ) ( ',' option )*
    snapshot       ::= <value to be used for timestamp>
    option         ::= sources | javadoc | pom | sign
    sources        ::= 'sources' 
                       ( ';path=' ( 'NONE' | PATH ) )?
                       ( ';force=' ( 'true' | 'false' ) )?
                       ( ';-sourcepath=' PATH ( ',' PATH )* )?
    javadoc        ::= 'javadoc'
                       ( ';path=' ( 'NONE' | PATH ) )?
                       ( ';packages=' ( 'EXPORTS' | 'ALL' ) )?
                       ( ';force=' ( 'true' | 'false' ) )?
                       ( ';' javadoc-option )*
    javadoc-option ::= '-' NAME '=' VALUE
    pom            ::= 'pom'
                       ( ';path=' ( 'JAR' | PATH ) )?
    sign            ::= 'sign'
                       ( ';passphrase=' VALUE )?

If `sources` or `javadoc` has the attribute `force=true`, either one will be release to the maven repository even if no `releaseUrl` or `snapshotUrl` is set or `maven-release=local`. 

The `aQute.maven.bnd.MavenBndRepository` is a bnd plugin that represent the local and a remote Maven repository. The locations of both repositories can be configured. The local repository is always used as a cache for the remote repository.

The repository has the following parameters:

* `url` – The remote repository URL. Credentials and proxy can be set with the [Http Client options]. The url should be the base url of a Maven repository. If no URL is configured, there is no remote repository.
* `local`  – (`~/.m2/repository`) The location of the local repository. By default this is `~/.m2/repository`. It is not possible to not have a local repository because it acts as the cache.
* `generate` – (`JAVADOC,SOURCES`) A combination of `JAVADOC` and/or `SOURCES`. If no `-maven-release` instruction is found and the released artifact contains source code, then the given classifiers are used to generate them.
* `readOnly` – (`false`) Indicates if this is a read only repository
* `name` – The name of the repository

If the Maven Bnd Repository is asked to put a file, it will look up the `-maven-release` instruction using merged properties. The property is looked up from the bnd file that built the artifact. However, it should in general be possible to define this header in the workspace using macros like `${project}` to specify relative paths.

# Signing

If the instruction contains the sign attribute  and release build is detected the repository tries to apply [gnupg](https://gnupg.org/) via a command process to create `.asc` files for all deployed artifacts. This requires a Version of [gnupg](https://gnupg.org/) installed on your build system. By default it uses the `gpg` command. If the `passphrase` is configured, it will hand it over to the command as standard input. The command will be constructed as follows: `gpg --batch --passphrase-fd 0 --output <filetosign>.asc --detach-sign --armor <filetosign>`. Some newer gnupg versions will ignore the passphrase via standard input for the first try and ask again with password screen. This will crash the process. Have a look [here](https://stackoverflow.com/questions/19895122/how-to-use-gnupgs-passphrase-fd-argument) to teach gnupg otherwise. The command can be exchanged or amended with additional options by defining a property named `gpg` in your workspace (e.g. `build.bnd` or somewhere in the ext directory).

Example config could look like:

```
# use the env macro to avoid to set the passphrase somehwere in your project
-maven-release: pom,sign;passphrase=${env;GNUPG_PASSPHRASE}
gpg: gpg --homedir /mnt/n/tmp/gpg/.gnupg --pinentry-mode loopback
```



 

[1]: /plugins/maven
